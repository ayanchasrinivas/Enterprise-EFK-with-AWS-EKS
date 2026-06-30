FROM python:3.11-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY dashboards/monitoring/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY dashboards/monitoring/* .
RUN useradd -m -u 1000 monitor
USER monitor
EXPOSE 8080 9090
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/api/status || exit 1

CMD ["python", "monitor.py"]