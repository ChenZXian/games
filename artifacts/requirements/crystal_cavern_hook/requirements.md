# Crystal Cavern Hook Requirements Draft

## Game Positioning
Crystal Cavern Hook is a Gold Miner inspired arcade hook game for Android Java. The player operates a compact mining winch above a luminous crystal cave, times a swinging claw, pulls valuable minerals and order items, avoids shatter hazards, and upgrades tools between short stages. It should feel familiar to players who like classic hook mining, but the design must stand apart through crystal refraction, layered order objectives, hazard timing, and a bright cavern visual identity.

## Target Feel And Player Fantasy
The player fantasy is becoming a clever cave prospector who reads the sparkle, weight, and danger of each crystal layer before firing the hook. The target feel is quick, readable, tactile, and satisfying: the claw swings with clear rhythm, the moment of launch matters, heavy crystals strain the cable, and a successful order chain feels crisp and rewarding.

## Core Gameplay Loop
1. A stage opens with a visible cavern board containing crystals, gold nuggets, relic chests, bombs, shard traps, and order targets.
2. The claw swings automatically from the top winch.
3. The player taps to launch the claw at the current angle.
4. The claw travels outward, may bend through prism crystals, catches the first valid target, and returns to the winch.
5. Returned items add coins, order progress, tool charges, or hazards depending on item type.
6. The player uses limited tools such as dynamite, magnet pulse, cable boost, prism lens, or shield glove to improve a bad pull or meet an order.
7. The stage ends when the timer expires or all required order goals are met.
8. Coins buy upgrades for cable speed, claw strength, tool stock, order bonuses, and prism control.

## Controls And Input Model
The main control is one-tap timing. Tap the playfield to fire the swinging claw. While the claw returns, the player can tap a boost button to speed up the cable if charges remain. Tool buttons sit in a small side pocket: dynamite destroys the caught object, magnet pulse attracts light gems, prism lens previews a bend path, and shield glove prevents one shard penalty. The control layout must keep the active cavern clear and avoid covering hook paths or collectible targets.

## Failure Conditions And Win Conditions
A stage is cleared by meeting the order target before the timer ends, such as collecting a value quota, pulling two blue crystals, retrieving a relic chest, or avoiding too many broken shards. A stage fails if the timer reaches zero before the order target is complete, if the player triggers too many shatter penalties, or if a special fragile order item breaks. Failed stages are retryable without losing permanent upgrades.

## Progression Structure
Progression is stage-based. Early stages teach basic angle timing and item weights. Mid stages add prism crystals, bombs, cracked stones, moving cave bats, and order combinations. Later stages add multi-layer caves, fragile relics, rare rainbow gems, chained prism bends, and timed cave tremors. Persistent upgrades include cable speed, claw grip, heavy-pull motor, tool capacity, prism lens duration, order reward multiplier, and lucky gem spawn chance.

## Level Or Run Structure
Each stage lasts about 60 to 90 seconds. A stage contains a fixed top winch, a layered cavern below, and a mix of static and moving targets. The cave has at least three depth bands: shallow light items, mid-value crystals, and deep heavy treasures. Later stages add side pockets, blocked chambers, and fragile clusters that reward careful angle reading. A full session should support quick mobile play with three to five stages per short run.

## Economy, Rewards, Drops, And Upgrades
Coins come from returned valuables and order completion. Crystal shards are a secondary resource for tool upgrades. Relic chests unlock one-time bonus rewards or cosmetic cave badges. Bombs can be hazards or useful pickups depending on whether they are pulled directly or triggered with the dynamite tool. The upgrade economy should push meaningful tradeoffs: faster cable for more pulls, stronger claw for heavy treasure, more tools for control, or higher order reward for score chasing.

## Gameplay Diversity And Content Budget
The genre family is arcade physics timing with a concrete sub-archetype of refracted hook mining and order-based cave collection. It is not a direct reskin of a classic hook miner baseline because the confirmed loop must add prism path bending, stage orders, shatter hazard management, tool decisions, and layered cave layouts instead of only aiming a hook and pulling loot.

The playfield is a portrait-leaning or landscape-safe cavern board with the winch at the top, a protected cavern body, a side tool pocket, and a bottom order rail. Minimum interactive regions are winch, swing arc, shallow gem band, mid crystal band, deep treasure band, prism bend cluster, hazard pocket, tool pocket, and order rail. Terrain elements include crystal walls, dark stone pockets, glowing cave floor, prism columns, cracked shard fields, and relic alcoves. Functional elements include collectible weights, hook collision targets, prism redirectors, bombs, fragile shards, moving bats, order markers, and return cable progress.

The minimum roster includes ten collectible families, five hazard families, five tool families, four stage objective families, and five feedback effects. Required mechanics include angle timing, hook travel, catch priority, weight-based return speed, prism bend preview, tool usage, order tracking, combo rewards, shatter penalties, and stage progression.

Forbidden template reuse: do not reduce the game to a classic hook miner core loop of aim hook pull loot only; do not copy ranch or farm management layouts; do not reuse tower defense lane maps; do not use a generic full-width top HUD plus bottom command strip if it covers the hook field; do not represent cave objects as plain colored circles without weight, value, or role feedback.

## Visual Identity Contract
The UI layout archetype is a luminous crystal cave console. The active hook field dominates the center, with the winch and timer integrated into a top cave beam, a compact tool pocket on the right, and an order rail carved into the lower cave wall. The HUD should show timer, order progress, coin total, tool charges, and current target value without covering the swing arc.

The palette signature is deep indigo cave shadow, emerald crystal, cyan glow, warm gold, amber lantern light, and magenta rare prism highlights. The material language is faceted crystal panels, brass winch parts, rope cable, carved stone labels, lantern glows, and translucent order chips. Typography should be bold, rounded, and readable with English-only labels.

Playfield safety is strict: the hook swing arc, outbound path, return path, and target field must stay free of decorative frames or buttons. UI panels may occupy reserved top, right, and bottom strips only. Toasts should appear near the winch or order rail, never over dense target clusters.

The icon subject is a split-tip brass mining hook gripping an emerald cave gem beside a nugget and cable spark. The silhouette should be a diagonal hook cable, forked claw hook, large faceted gem, and nugget cluster. It must not reuse a generic shield, sword, castle, farm crate, medical kit, animal face, or the old basic gold hook icon.

## Screen Map
- Menu: title, start button, sound toggle, help button, cave depth badge, and a slow swinging hook preview.
- Gameplay HUD: top winch beam, timer, order chip, coin counter, active hook, cavern target field, right tool pocket, and bottom order rail.
- Tool Shop: cable speed, claw grip, motor strength, magnet pulse, dynamite stock, shield glove, and prism lens upgrades.
- Pause: resume, restart stage, sound toggle, and return to menu.
- Result: order status, coins, bonus shards, best pull, stage stars, and next stage or retry.
- Help: one-page diagram explaining swing timing, item weight, prism bends, tools, hazards, and orders.

## UI Direction
The recommended `ui_skin` is `skin_cartoon_light`, customized with crystal cave panels and brass tool buttons. The UI must be brighter and more jewel-like than dark arcade or military skins. HUD priorities are timer, order progress, current coins, tool charges, and hook state. The playfield safe area should reserve the top 12 percent for winch and timer, the right 16 percent for tools, and the bottom 14 percent for order progress while keeping the central cavern target field unobstructed.

## Gameplay Art Direction
The camera perspective is a fixed 2D side cutaway cave. Required art roles are winch, cable, claw, gold nugget, small crystal, large crystal, prism crystal, relic chest, stone lump, cracked shard, bomb, bat hazard, magnet pulse, dynamite burst, sparkle, shatter effect, and order markers. Objects should have readable silhouettes and value tiers: light gems are small and bright, heavy gold is bulky, relic chests are rectangular with metal bands, bombs are round with fuse sparks, and prism crystals are tall angular shapes.

The claw faces and rotates along its travel direction. Moving bats should flap between two poses. The claw needs idle swing, launch, catch, return, and drop states. Collectibles need idle sparkle, caught, pulled, delivered, shattered, and bonus states. Effects should include cable tension, impact pop, prism bend flash, coin burst, and shatter dust. Anchors are object center for collision, lower center for grounded heavy objects, and cable tip for the claw. Hitboxes should be slightly smaller than visual bounds so near misses feel fair.

If the shared library lacks enough cave and crystal assets, the gameplay art workflow may use project-local generated canvas art for first delivery, but delivery-ready output must still define runtime art roles and avoid plain placeholder circles.

## Icon Direction
The icon should show a brass claw gripping a large emerald crystal, with a gold nugget, cable spark, and dark cave badge background. The color direction is emerald, cyan, warm gold, brass, and dark indigo. The tone is premium, shiny, playful, and immediately readable as a hook mining game.

## BGM Direction
Menu music should be gentle cave ambience with soft mallets, low warm pads, and occasional crystal chimes. Gameplay music should be lightly rhythmic with plucked percussion and a ticking mining tempo. Result music should use short coin and crystal flourish stings. SFX should cover hook fire, cable return, item catch, heavy strain, coin delivery, crystal sparkle, prism bend, shatter, dynamite, magnet pulse, order complete, and stage fail.

## Technical Implementation Notes
Implementation should be a single Android Java project under `games/crystal_cavern_hook` after confirmation. Use `com.android.boot.MainActivity`, a custom Java game view for hook physics and drawing, and Android View/XML overlays for menu, pause, shop, help, and result screens. Required state model includes MENU, PLAYING, PAUSED, SHOP, RESULT, and GAME_OVER. Important systems are swing angle update, hook launch and return states, target collision priority, item weight values, prism path bending, tool charges, stage order generation, timer pressure, upgrade persistence, and runtime art mapping.

## Differentiation Note Against Registry
Compared with a classic hook miner baseline, this concept keeps the familiar hook timing appeal but requires new systems: prism refraction, order objectives, layered cave budgets, hazard penalties, tool pockets, and upgrade choices. Compared with rail defense games, it is not rail defense or tower placement. Compared with ranch and farm games, it has no animal care, crop management, feed recipes, market pricing, or delivery contracts. Compared with action and runner entries, it is a fixed-screen precision timing game rather than movement combat or lane running.

## Confirmation Gate
This requirements trace is a draft. Project initialization must not begin until the user explicitly confirms these requirements.
