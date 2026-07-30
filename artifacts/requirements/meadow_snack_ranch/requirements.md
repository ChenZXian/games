# Meadow Snack Ranch Requirements Draft

## Game Positioning
Meadow Snack Ranch is a casual Android Java ranch management game inspired by the familiar comfort of QQ Ranch, but its main decision is feed recipe matching. The player mixes named snack feeds, gives them to recognizable farm animals, collects quality-based products, and completes delivery orders. The game should feel polished, bright, and touch-friendly, with attractive UI and animal shapes that clearly resemble real cows, hens, sheep, goats, pigs, and rabbits instead of abstract placeholders.

## Target Feel And Player Fantasy
The player fantasy is running a tidy meadow ranch where animals respond to carefully chosen snacks. The mood is cozy, lively, and rewarding: every action should create visible feedback such as chewing, happy poses, quality stars, stamped order cards, and a calmer ranch board that feels worth returning to.

## Core Gameplay Loop
1. Restock or harvest simple ingredients such as corn, clover, oats, carrot, apple, and herbs.
2. Use the feed mixer to create snack recipes.
3. Drag one recipe card to a matching animal pen.
4. Wait through a short production timer while animal mood and recipe fit affect quality.
5. Collect products such as milk, eggs, wool, goat milk, truffles, and soft fur.
6. Store fresh products in a cooler or fulfill delivery orders.
7. Earn coins and reputation, then unlock new recipes, animals, pen comfort upgrades, and extra order slots.

## Controls And Input Model
The game uses direct mobile touch. Tap animals or buildings to inspect readiness, drag feed recipe cards from the left cookbook rail to animal pens, short-hold a ready pen for batch collection, tap order cards on the right clipboard to deliver, and tap upgrade signs to spend coins. Touch targets must stay large and clear on phone screens.

## Failure Conditions And Win Conditions
Each day has a reputation target and a soft timer. The player wins the day by reaching the target reputation before the day ends. A day fails if the timer ends with reputation below target, if too many products spoil from cooler overflow, or if animal mood stays too low for too long. The game is forgiving: failed days can be retried without losing permanent unlocks.

## Progression Structure
Progression is day-based. Early days teach feeding and collection with cow and hen only. Mid-game adds sheep, goat, piglet, and rabbit plus recipe preferences. Later days introduce premium orders, freshness pressure, rare ingredients, and temporary visitor animals such as prize cow and golden hen. Persistent upgrades include mixer speed, ingredient capacity, pen comfort, cooler freshness duration, and order board slots.

## Level Or Run Structure
The base mode uses short ranch days lasting roughly 3 to 5 minutes. Each day has a small set of order goals and one modifier: sunny easy day, rainy comfort penalty, lunch rush, festival snack request, or rare animal visit. The ranch board remains familiar, but order requirements, ingredient supply, animal preferences, and freshness pressure vary by day.

## Economy, Rewards, Drops, And Upgrades
Coins come from deliveries. Reputation comes from high-quality orders, recipe matches, and streaks. Ingredients refill on timers and can be expanded through upgrades. Premium recipe coupons are rare rewards used to unlock stronger snack recipes. Product quality tiers are standard, fresh, premium, and perfect. Better recipes and happier animals increase the chance of premium products.

## Gameplay Diversity And Content Budget
The genre family is casual management, with a concrete sub-archetype of feed recipe animal ranch. It is not a reskin of Pasture Parade Deluxe because it avoids generic feed and cleanliness maintenance as the central loop; instead, the player makes recipe-fit decisions that change product quality and order value. It is also not a reskin of Market Morning Farm because there is no crop stall pricing or customer queue.

The playfield is a three-quarter top-down ranch board with at least seven interactive regions: ingredient beds, feed mixer counter, cow meadow, chicken coop, sheep paddock, goat corner, and delivery board. Terrain and map elements must include meadow grass, barn paths, straw floors, flower borders, trough edges, a snack barn landmark, collection crates, ingredient bins, a cooler chest, and upgrade signs.

The roster target includes six normal animals, two rare visitor animals, six ingredient types, six snack recipe items, six product families, and five feedback effects. The mechanic target includes feed dragging, recipe crafting, product collection, delivery fulfillment, freshness storage, animal mood, recipe preference matching, order reroll, and permanent upgrades.

Forbidden template reuse: do not copy the Pasture Parade Deluxe adjacency combo loop, do not copy the Market Morning Farm crop market loop, do not use a tiny single generic pasture, and do not represent animals as circles, rectangles, or label-only icons.

## Visual Identity Contract
The UI layout archetype is a storybook ranch board with a left cookbook recipe rail and a right delivery clipboard. The HUD uses a compact upper-left day clock and coin badge, an upper-center reputation ribbon, and a small cooler freshness dock outside the active animal pens. Navigation uses anchored panels for recipes, orders, upgrades, pause, and result screens.

The palette signature is mint meadow green, milk white, barn red, sunflower yellow, and small sky blue accents. The material language is painted wood, paper recipe cards, stitched cloth badges, and soft shadows. Typography should be rounded and friendly but readable in English.

Playfield safety is strict: the central ranch board must keep animal pens, ingredient beds, mixer drop zone, and collection crate path free from decorative overlays. Frames belong only in the reserved side panels and dead space.

The icon subject is a friendly realistic dairy cow head beside a wooden feed bowl with colorful snack pieces and a small recipe tag. The silhouette must show muzzle, ears, small horns, and bowl shape. It must not reuse the Market Morning Farm crate icon, a generic barn-only icon, or a simple animal blob.

## Screen Map
- Menu: ranch title, start button, continue button if save exists, sound toggle, help button, and a soft animated ranch background.
- Gameplay HUD: central ranch board, left cookbook recipe rail, right delivery clipboard, compact top HUD, cooler dock, and temporary toast feedback.
- Recipe Book: list of unlocked snacks, required ingredients, favorite animals, and expected product quality bonus.
- Upgrade Sheet: mixer, ingredient bins, pen comfort, cooler, and order board upgrades.
- Pause: resume, restart day, sound toggle, and return to menu.
- Result: day outcome, coins, reputation, best product quality, unlocked recipe or upgrade, restart or next day.
- Help: one-page explanation of drag feed, collect products, freshness, and orders.

## UI Direction
The recommended `ui_skin` is `skin_cartoon_light`. The final UI must be better than bare UI Kit scaffolding. It should use a cookbook rail, clipboard order board, stamped reward feedback, milk-bottle freshness meter, and wood-tab headers. The layout should feel like a polished cozy ranch tool surface rather than a generic arcade panel. Large buttons and recipe cards are required for touch clarity.

## Gameplay Art Direction
The camera perspective is three-quarter top-down. Gameplay art must use realistic animal silhouettes with stylized friendly rendering. A cow needs a long body, clear head, ears, small horns, muzzle, legs, and udder; a hen needs beak, wing, tail, and legs; a sheep needs wool mass and visible face/legs; a goat needs horns and beard; a pig needs snout and short legs; a rabbit needs long ears and compact body. Simple circles, capsules, or rectangles are not acceptable for delivery-ready output.

Required visual roles are animals, pens, ingredient beds, feed mixer, ingredient bins, product crates, cooler, order board, snack recipe cards, and feedback effects. Primary animals need idle, walk, eat, happy, ready-to-collect, and low-mood states. Moving animals should face their movement direction by left/right frame selection or controlled flipping, with front/back variants preferred if the selected art supports them. Product-ready animals should show a visible state marker integrated with the animal or pen.

Anchors should be at the lower center of each animal body. Hitboxes should cover the body and pen interaction area without requiring precise taps. Z-order should place ground and pen floors first, animals above pen floors, feed and product effects above animals, and UI overlays above gameplay only in reserved safe zones.

If the shared library lacks suitable animal art, the gameplay art workflow must import or generate license-clear animal sprites through the approved repository art workflow before delivery. Prototype-only shapes are allowed only during early internal testing, not for the final requested quality.

## Icon Direction
The icon should be cartoon-polished but grounded in real animal shape: a cow head with clear muzzle, ears, small horns, and black patches, plus a wooden feed bowl and recipe tag. The color direction is mint meadow, milk white, barn red, and sunflower yellow. The tone is cheerful and premium.

## BGM Direction
Menu music should be warm, bright, and relaxed, with light acoustic plucks and gentle bell tones. Gameplay loop music should be slightly more rhythmic but still cozy, matching short ranch work sessions. A rush-day variation may add hand percussion and a faster tempo. SFX should cover feed mix, drag drop, animal happy response, product collect, order stamp, upgrade, error, day clear, and day fail.

## Technical Implementation Notes
Implementation should be a single Android Java project under `games/meadow_snack_ranch` after confirmation. The game should use `com.android.boot.MainActivity`, a custom Java game view for the ranch board, and Android View/XML overlays for menu, HUD, pause, help, upgrades, and result screens. The state model should include MENU, PLAYING, PAUSED, and GAME_OVER or RESULT.

Important systems to plan early are recipe matching, product quality calculation, freshness decay, animal mood, day modifiers, order generation, persistent upgrades, and a runtime gameplay art map for animal roles, facing, anchors, hitboxes, z-order, states, and movement rules.

## Differentiation Note Against Registry
Compared with `pasture_parade_deluxe`, this game must focus on recipe-to-animal matching, quality output, freshness, and order composition rather than generic pen maintenance and adjacency combo growth. Compared with `market_morning_farm`, it removes market pricing and customer queue management, replacing them with animal feeding, product quality, and delivery orders. Compared with crop and flower farm entries, it centers on realistic animal shapes and feed recipes rather than planting and harvesting as the main fantasy.

## Confirmation Gate
This requirements trace has been confirmed by the user. Downstream workflows should preserve the confirmed feed-recipe ranch loop, cookbook and clipboard UI identity, and realistic animal silhouette requirement.
