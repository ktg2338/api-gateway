RESOURCE_GROUP="aif-prod-rg"
LOCATION="koreacentral"
REGISTRY_NAME="aifprodacr"
ENV_NAME="aif-prod-env"
CONTAINER_APP_NAME="gateway-prod"
LOG_WORKSPACE_NAME="aif-prod-workspace"
APP_INSIGHTS_NAME="aif-prod-insights"

echo "Creating Application Insights..."
az monitor app-insights component create \
  --app $APP_INSIGHTS_NAME \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP \
  --workspace aif-prod-workspace

echo "Creating Container App..."
az containerapp create \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment $ENV_NAME \
  --image mcr.microsoft.com/azuredocs/containerapps-helloworld:latest \
  --target-port 80 \
  --ingress 'external' \
  --min-replicas 1 \
  --cpu 1.0 \
  --memory 2.0Gi \
  --query properties.configuration.ingress.fqdn

echo "Creating service principal for ACR access..."
ACR_LOGIN_SERVER=$(az acr show --name $REGISTRY_NAME --resource-group $RESOURCE_GROUP --query loginServer --output tsv)
SERVICE_PRINCIPAL=$(az ad sp create-for-rbac --name "$CONTAINER_APP_NAME-acr-sp" --role acrpull --scope $(az acr show --name $REGISTRY_NAME --resource-group $RESOURCE_GROUP --query id --output tsv))



