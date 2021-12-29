plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("install_time_asset_pack") // Directory name for the asset pack
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
