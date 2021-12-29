plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("fast_follow_asset_pack") // Directory name for the asset pack
    dynamicDelivery {
        deliveryType.set("fast-follow")
    }
}
