{{/*
Expand the name of the chart.
*/}}
{{- define "certificate-manager.name" -}}
{{- .Values.fullnameOverride | default .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to all resources.
*/}}
{{- define "certificate-manager.labels" -}}
app: {{ include "certificate-manager.name" . }}
app.kubernetes.io/name: {{ include "certificate-manager.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{/*
Selector labels for Deployment / Pod matchLabels.
*/}}
{{- define "certificate-manager.selectorLabels" -}}
app: {{ include "certificate-manager.name" . }}
app.kubernetes.io/name: {{ include "certificate-manager.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Name of the Secret that holds sensitive env vars.
Uses existingSecret.name when provided, otherwise falls back to
the chart-managed secret named "<release>-secrets".
*/}}
{{- define "certificate-manager.secretName" -}}
{{- if .Values.existingSecret.name -}}
{{ .Values.existingSecret.name }}
{{- else -}}
{{ include "certificate-manager.name" . }}-secrets
{{- end }}
{{- end }}
