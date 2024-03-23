import xyz.jpenilla.resourcefactory.bukkit.Permission

description = "Banners addon for squaremap"

bukkitPluginYml {
    main = "xyz.jpenilla.squaremap.addon.banners.SquaremapBanners"
    authors.add("BillyGalbreath")
    authors.add("granny")

    permissions {
        register("squaremap.banners.admin") {
            description = "Allow controlling the plugin"
            default = Permission.Default.OP
        }
    }
}
