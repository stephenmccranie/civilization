# Map and discovery — first rules draft

Status: explored terrain, public shop locations/offers, inspectable property ownership/boundaries, private prospecting discoveries unless shared, and no public live player/shipment tracking are agreed. Basic search → inspect offer → waypoint → physical travel is agreed direction. Detailed UI, publication, sharing and synchronization mechanics remain proposals. No map feature or technical foundation is implemented or selected.

Related: [currency_and_exchange.md](currency_and_exchange.md), [region_system.md](region_system.md), [property_and_authority.md](property_and_authority.md).

## Purpose

Help players find business, choose places and navigate physical deliveries. Make public commercial information easy to discover without automatically revealing private activity or finite deposits.

## Visibility matrix

| Information | Visibility direction | Boundary |
| --- | --- | --- |
| Terrain | Personal exploration reveals terrain | No automatic fully revealed world; exploration-sharing is a later choice |
| Public shops | Listed locations visible even outside explored terrain | A shop marker need not reveal its surrounding terrain |
| Shop offers | Goods, quantities, fuel denomination, prices and availability | Listing visibility does not reserve stock or permit remote pickup |
| Property | Query a parcel boundary and owner at a selected location | Ownership records do not expose inventories, residents or revenues; legal boundaries can be known without revealing terrain |
| Agricultural/forestry suitability | Terrain provides clues; agreed local inspection supplies precise feedback | Detailed regional overlays are optional later work |
| Mineral terrain | Visible landscape guides prospecting | No map of every underground reserve |
| Discovered deposits | Private annotations for the discoverer initially | Sharing with others is a separate deliberate action; exact discovery data remains open |
| Other players and vehicles | No public live tracking by default | Shop discovery must not become a shipment-tracking or surveillance system |
| Settlements and landmarks | Manual player waypoints initially | Public naming/registration can be designed later; no settlement bureaucracy required |

The core visibility rules are agreed. Public shop coordinates on unexplored terrain are intentional: easy market discovery and personal terrain exploration answer different needs. A global directory discloses a commercial location; explicit publication controls, annotation sharing, suitability overlays and landmark registration still need decisions. Exact parcel representation also remains open.

## Small first interaction

Open the map, filter buy or sell offers by item, select a shop and read its offer. Set a waypoint and travel there. Complete the exchange at the physical counter. Show prices in the listed fuel; do not rank unlike fuel prices as directly comparable without an exchange rule.

Start with markers, a short offer panel, item filtering and waypoints. Route finding, predicted profits, transport dispatch and automated reservations are not needed for this interaction. A waypoint indicates a destination, not a guarantee of a navigable river route or access through private land.

Proposed availability display: in stock, partly fulfillable, out of stock or unavailable, with a quantity where useful. Read the offer's saleable stock or funded demand, not the shop's entire inventory. An unavailable listing must not promise completion. Owner logout alone does not close a shop.

## Correctness and maintenance

Do not force-load shop chunks to populate the map. Keep a persistent listing index with an explicit freshness/unavailable policy, update it from authoritative shop events and recheck at purchase. Exact indexing, batching and stale-listing cleanup remain technical design work.

Removing a shop, changing permissions, changing stock or cancelling an offer must eventually update its listing. Decide whether an unloaded shop reports last-known availability or a safely maintained current figure; do not imply a guaranteed remote reservation.

Property boundaries depend on the eventual parcel model. Private deposit annotations need persistence and access control. Use paged or filtered results rather than transmitting every detailed listing continuously to every player. These are constraints to validate, not evidence of 200-player readiness.

## Next decisions

The core visibility matrix is settled. Return to land acquisition and retention with the now-defined market and physical energy currencies. Exact map artwork, key bindings and rendering technology can wait. Do not treat open details such as publication controls or sharing mechanics as already approved.
