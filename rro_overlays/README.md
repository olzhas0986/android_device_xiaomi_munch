# munch overlays

All overlays are local to this device tree. Resource values from the former
platform overlays are merged into the device overlays; munch values take
precedence where both define a resource with the same name and qualifier.

Modules use `[Lineage]<component>OverlayDevice` and package names
`<target package>.overlay[.lineage].device`. AOSP overlays use priority 200;
overlays for LineageOS additions use priority 250.
