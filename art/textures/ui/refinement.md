# Cabinet resolution refinement

Preserve the approved cabinet master, soft tan enamel, dark metal border and brass corner rivets. The task is fidelity correction, not a new design. Original mockup/prompt/source provenance remains in prompt.md and receipt.json.

Landmarks: eighteen-GUI-pixel corner region and existing slot positions; rounded faceted brass heads, tiny light glints, dark lower rims, layered iron bevel. Use 512×512 RGB (48/18 source texels per logical GUI pixel at corners) and proportional nine-slice coordinates. No new generated master is needed: the previously approved generated source already contains the requested detail. Reuse its original direction and inspect actual inventory/chest/machine screens.

Runtime output: shared cabinet.png and MachineUi.panel. All consumers inherit the refinement; item textures and Minecraft text retain their own resolution. Low GUI scales necessarily show less texture detail. Native slots and all hitboxes remain unchanged.

Mockup-02 recipe-browser refinement: heavier shared frame and rivets, dark inset category tabs with brass selection, beveled brass title plate with fasteners, and magnifying-glass search field with muted placeholder text. Use existing source art and native pixel geometry; no new generated master.

The shared frame expands three GUI pixels outward, keeping enlarged rivets clear of native slots. The raised title plate ends before the chest first row and industrial labels.
