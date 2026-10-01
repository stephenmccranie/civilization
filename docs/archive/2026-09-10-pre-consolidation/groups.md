# Solo and trusted-group ownership

Status: solo play and small trusted groups of roughly 2–8 players are the expected baseline. The user chose full access for group members. This is a usage expectation, not an approved eight-player membership cap. No group system is implemented.

## Agreed access direction

Members have full access to the group's shared claims, storage, machines, shops and vehicles. Avoid individual permissions and custom role trees for ordinary members. Full access includes building/dismantling, operating assets, depositing/withdrawing goods, managing offers and using shared fuel, subject to global game rules.

Group access does not bypass a pending takeover's controller-energy lock, claim power requirements or another owner's rights. A trusted member can spend or remove shared resources; ordinary authorized use is not a permission exploit. Design around actual trust rather than silently protecting each member's contribution.

## Minimal proposed lifecycle

- Solo players own assets directly without creating an organization.
- One persistent group roster supports invitations, accepted joins and leaving.
- Joining grants access to all group-owned assets; no per-member assignment is needed.
- Personal assets do not automatically become shared on joining. Deliberately assign them to the group; members then have full access.
- Leaving or removal ends group access, including open menus and future vehicle/storage actions. It does not automatically divide group assets or recover previously withdrawn items.
- Recommended owner-only administrative powers: transferring ownership, removing members, transferring leadership and disbanding. These were proposed separately from ordinary asset access; confirm the administrative boundary before coding.
- Leadership transfer and absence need simple explicit rules. Disbanding must resolve group assets and pending obligations rather than silently deleting or orphaning them.

Personal-versus-group assignment, invitation authority, multiple membership and owner-only administration remain detailed proposals. Full access to shared assets is agreed. Group name and leader are sufficient candidate identity fields; no equity, payroll, elections, ranks or formal jurisdiction is required for the initial design.

## Example: a timber group

Members of a four-person group can harvest its forest holding, withdraw timber, load the boat, operate the shop and spend shared fuel. An outsider interacts through a trade offer without gaining access to the warehouse. A member leaving loses access to remaining shared assets; assets stay with their current owner.

## Implementation connections

Use one ownership reference (player or group) across claims, shops, storage and vehicles. Resolve current membership for permission checks; do not copy a permanent member list into every asset. Membership changes must take effect for already-open interactions and persist through restart. Define how an occupied vehicle behaves on access revocation.

Existing dev assets require explicit adoption, not automatic assignment based on whoever loads a chunk first. Asset transfers must preserve inventory, controller reserve and funded obligations. Administrative recovery needs an audit trail; ordinary calorie/machine logs are not a complete ownership database.

## Next planning connection

Specify owner-only administration and personal-to-group assignment briefly, then return to the whole-mod coverage map. Do not expand full-access groups into custom ranks or workforce management without a demonstrated need. Larger-scale institutions and inter-group agreements can remain future questions.
