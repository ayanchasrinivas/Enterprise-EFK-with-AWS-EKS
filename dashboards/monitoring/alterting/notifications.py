"""
Notification backends: Slack, PagerDuty, Email, AWS SNS.
Each implements a common .send(alert) interface.
"""
from __future__ import annotations

import json
import smtplib
import ssl
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from typing import Optional

import boto3
import requests
from slack_sdk.webhook import WebhookClient

from config import ALERT_CONFIG
from alerting.alert_manager import Alert, Severity
from utils.logger import get_logger

log = get_logger(__name__)

SEVERITY_EMOJI = {
    Severity.INFO: ":information_source:",
    Severity.WARNING: ":warning:",
    Severity.CRITICAL: ":fire:",
}

SEVERITY_COLOR = {
    Severity.INFO: "#36a64f",
    Severity.WARNING: "#ffb300",
    Severity.CRITICAL: "#e01e5a",
}


class SlackNotifier:
    """Sends rich Slack Block Kit messages via an Incoming Webhook."""

    def __init__(self, webhook_url: Optional[str] = None, channel: Optional[str] = None):
        self._webhook_url = webhook_url or ALERT_CONFIG.slack_webhook_url
        self._channel = channel or ALERT_CONFIG.slack_channel
        self._client = WebhookClient(self._webhook_url) if self._webhook_url else None

    def send(self, alert: Alert) -> None:
        if not self._client:
            log.warning("Slack webhook URL not configured")
            return

        emoji = SEVERITY_EMOJI.get(alert.severity, ":bell:")
        color = SEVERITY_COLOR.get(alert.severity, "#cccccc")

        blocks = [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "text": f"{emoji} {alert.title}",
                },
            },
            {
                "type": "section",
                "fields": [
                    {"type": "mrkdwn", "text": f"*Severity:*\n{alert.severity.upper()}"},
                    {"type": "mrkdwn", "text": f"*Component:*\n{alert.component.capitalize()}"},
                    {"type": "mrkdwn", "text": f"*Time:*\n{alert.timestamp.strftime('%Y-%m-%d %H:%M:%S UTC')}"},
                    {"type": "mrkdwn", "text": f"*Alert ID:*\n`{alert.alert_id}`"},
                ],
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*Details:*\n{alert.message}",
                },
            },
        ]

        if alert.metadata:
            meta_text = "\n".join(f"• `{k}`: {v}" for k, v in list(alert.metadata.items())[:5])
            blocks.append({
                "type": "section",
                "text": {"type": "mrkdwn", "text": f"*Metadata:*\n{meta_text}"},
            })

        blocks.append({"type": "divider"})

        response = self._client.send(
            text=f"{emoji} {alert.severity.upper()}: {alert.title}",
            attachments=[{
                "color": color,
                "blocks": blocks,
            }],
        )
        if response.status_code != 200:
            log.error("Slack notification failed",
                      extra={"status": response.status_code, "body": response.body})


class PagerDutyNotifier:
    """Fires PagerDuty events via Events API v2."""

    PAGERDUTY_EVENTS_URL = "https://events.pagerduty.com/v2/enqueue"

    def __init__(self, routing_key: Optional[str] = None):
        self._key = routing_key or ALERT_CONFIG.pagerduty_routing_key

    def send(self, alert: Alert) -> None:
        if not self._key:
            log.warning("PagerDuty routing key not configured")
            return

        severity_map = {
            Severity.INFO: "info",
            Severity.WARNING: "warning",
            Severity.CRITICAL: "critical",
        }

        payload = {
            "routing_key": self._key,
            "event_action": "trigger",
            "dedup_key": alert.alert_id,  # prevents duplicate incidents
            "payload": {
                "summary": alert.title,
                "severity": severity_map.get(alert.severity, "warning"),
                "source": f"elk-monitor/{alert.component}",
                "timestamp": alert.timestamp.isoformat(),
                "custom_details": {
                    "message": alert.message,
                    "alert_id": alert.alert_id,
                    **alert.metadata,
                },
            },
        }

        resp = requests.post(
            self.PAGERDUTY_EVENTS_URL,
            json=payload,
            timeout=10,
        )
        if resp.status_code not in (200, 202):
            log.error("PagerDuty notification failed",
                      extra={"status": resp.status_code, "body": resp.text})

    def resolve(self, alert_id: str) -> None:
        if not self._key:
            return
        payload = {
            "routing_key": self._key,
            "event_action": "resolve",
            "dedup_key": alert_id,
        }
        requests.post(self.PAGERDUTY_EVENTS_URL, json=payload, timeout=10)


class EmailNotifier:
    """Sends HTML email alerts via SMTP."""

    def __init__(self):
        self._host = ALERT_CONFIG.email_smtp_host
        self._port = ALERT_CONFIG.email_smtp_port
        self._from = ALERT_CONFIG.email_from
        self._to = [e for e in ALERT_CONFIG.email_to if e]
        self._password = ALERT_CONFIG.email_password

    def send(self, alert: Alert) -> None:
        if not self._from or not self._to:
            return

        color = SEVERITY_COLOR.get(alert.severity, "#cccccc")
        html = f"""
        <html><body>
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <div style="background-color: {color}; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
            <h2 style="margin: 0;">{alert.severity.upper()}: {alert.title}</h2>
          </div>
          <div style="background: #f8f9fa; padding: 20px; border: 1px solid #e0e0e0;">
            <table style="width: 100%; border-collapse: collapse;">
              <tr><td style="padding: 8px; font-weight: bold;">Component</td>
                  <td style="padding: 8px;">{alert.component}</td></tr>
              <tr><td style="padding: 8px; font-weight: bold;">Time</td>
                  <td style="padding: 8px;">{alert.timestamp.strftime('%Y-%m-%d %H:%M:%S UTC')}</td></tr>
              <tr><td style="padding: 8px; font-weight: bold;">Alert ID</td>
                  <td style="padding: 8px; font-family: monospace;">{alert.alert_id}</td></tr>
            </table>
            <hr style="margin: 20px 0;">
            <h3>Details</h3>
            <p>{alert.message}</p>
            {"<h3>Metadata</h3><pre>" + json.dumps(alert.metadata, indent=2) + "</pre>" if alert.metadata else ""}
          </div>
        </div>
        </body></html>
        """

        msg = MIMEMultipart("alternative")
        msg["Subject"] = f"[{alert.severity.upper()}] ELK Monitor: {alert.title}"
        msg["From"] = self._from
        msg["To"] = ", ".join(self._to)
        msg.attach(MIMEText(html, "html"))

        try:
            context = ssl.create_default_context()
            with smtplib.SMTP(self._host, self._port) as smtp:
                smtp.ehlo()
                smtp.starttls(context=context)
                smtp.login(self._from, self._password)
                smtp.sendmail(self._from, self._to, msg.as_string())
        except Exception as exc:
            log.error("Email notification failed", extra={"error": str(exc)})


class SNSNotifier:
    """Publishes alerts to an AWS SNS topic (triggers Lambda, email subscription, etc.)."""

    def __init__(self, topic_arn: Optional[str] = None, region: str = "us-east-1"):
        self._topic_arn = topic_arn or ALERT_CONFIG.sns_topic_arn
        self._client = boto3.client("sns", region_name=region) if self._topic_arn else None

    def send(self, alert: Alert) -> None:
        if not self._client or not self._topic_arn:
            return
        message = json.dumps({
            "alert_id": alert.alert_id,
            "severity": alert.severity,
            "component": alert.component,
            "title": alert.title,
            "message": alert.message,
            "timestamp": alert.timestamp.isoformat(),
            "metadata": alert.metadata,
        })
        self._client.publish(
            TopicArn=self._topic_arn,
            Message=message,
            Subject=f"[{alert.severity.upper()}] {alert.title}",
            MessageAttributes={
                "severity": {"DataType": "String", "StringValue": alert.severity},
                "component": {"DataType": "String", "StringValue": alert.component},
            },
        )
