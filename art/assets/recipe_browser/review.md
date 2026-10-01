# Recipe browser review

Mockup-02 is the approved direction; mockup-01 remains provenance.

Implemented in shared MachineScreen and RecipeWidget using existing cabinet art and real resource-pack item sprites. Comparison targets are the outer category column, horizontal material chips, compact six-row Tools / Iron list, brass selection and separate Automatic/Repair actions. Native font and 18-pixel rows deliberately replace the illustration’s enlarged typography. Machine slots and processing remain unchanged.

Initial visual check caught the client menu’s late kind synchronization; the catalog now rebuilds when that value arrives. The final interaction fixture covers category/material intersection, six iron tools, search-key handling, server selection, upgrade labels, ghost custody, scrolling, collapse and resize. Simple Tannery/Textile menus omit irrelevant filters. Missing-material recipes remain visible. The scrollbar is an indicator; use the wheel or Page Up/Down to navigate.

See comparison.png and implementation-receipt.json for final evidence and limits.

Cabinet refinement: thicker outward frame, larger rivets, dark inset category tabs, raised fastened brass nameplate and search icon/muted hint now bring the native implementation closer to the mockup. Existing generated cabinet master remains the source; no new raster generation.
