{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "flowable.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "flowable.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "flowable.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Template name.
*/}}
{{- define "flowable.template" -}}
{{- .Template.Name | replace "flowable/templates/" "" | trimSuffix ".yaml" | trunc 63 -}}
{{- end -}}

{{/*
Template name including release name.
*/}}
{{- define "flowable.templatefull" -}}
{{- printf "%s-%s" .Release.Name .Template.Name | replace "flowable/templates/" "" | trimSuffix ".yaml" | trunc 63 -}}
{{- end -}}

{{/*
Template default labels
*/}}
{{- define "flowable.defaultlabels" -}}
    app.kubernetes.io/name: {{ include "flowable.template" . }}
    helm.sh/chart: {{ include "flowable.chart" . }}
    app.kubernetes.io/instance: {{ .Release.Name }}
    app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Template default Ingress rules
*/}}
{{- define "flowable.ingressRules" -}}
    paths:
    {{- if .Values.ui.enabled  }}
      - path: {{ .Values.ui.ingressPath }}
        backend:
          serviceName: {{ .Values.ui.service.name }}
          servicePort: 8080
    {{- end }}
    {{- if .Values.rest.enabled }}
      - path: {{ .Values.rest.ingressPath }}
        backend:
          serviceName: {{ .Values.rest.service.name }}
          servicePort: 8080
    {{- end }}
{{- end -}}
