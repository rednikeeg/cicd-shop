output "resource_group_rg_id" {
  value = azurerm_resource_group.rg.id
}

output "resource_group_tfstate_id" {
  value = azurerm_resource_group.tfstate.id
}

output "storage_account_name" {
  value = azurerm_storage_account.tfstate.name
}