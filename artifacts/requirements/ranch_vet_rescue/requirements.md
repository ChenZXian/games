# Ranch Vet Rescue Requirements Draft

## Game Positioning
Ranch Vet Rescue is a casual Android Java ranch management game inspired by the familiar comfort of QQ Ranch, but its main decision is veterinary care sequencing. The player receives real-looking farm animals with visible conditions, diagnoses their needs, assigns treatment cards, moves them through care stations, and releases recovered animals for reputation, coins, and product bonuses. It should feel warm, helpful, and polished rather than clinical or dark.

## Target Feel And Player Fantasy
The fantasy is becoming the trusted pasture veterinarian for a small community ranch. The player should feel clever for reading animal cues, calm under gentle time pressure, and rewarded when a cow, sheep, hen, goat, piglet, or rabbit visibly recovers. The game should preserve cute ranch charm while requiring more judgement than a simple feed-and-collect loop.

## Core Gameplay Loop
1. New animals arrive at the intake lane with condition icons such as hungry, tired, scratched, chilled, dirty, or stressed.
2. The player taps or drags each animal card into one of several care stations: checkup table, feed tonic trough, grooming brush, warm blanket stall, medicine shelf, or recovery meadow.
3. Each station consumes a matching treatment card and starts a short timer.
4. Correct treatment increases health, mood, and trust; wrong treatment wastes time and raises stress.
5. Recovered animals move to the release gate and grant coins, reputation, and occasional product gifts.
6. Between waves, the player upgrades station speed, card capacity, animal comfort, and assistant slots.
7. Later days introduce mixed symptoms, priority animals, rare visitors, and ranch events.

## Controls And Input Model
The game uses direct mobile touch. Tap an animal to inspect its condition, drag animal cards to care stations, tap treatment cards to select or queue them, tap completed stations to release animals, and tap upgrade tabs between days. All animal and station hit targets must remain large enough for phone screens. No precise dragging to tiny targets is allowed.

## Failure Conditions And Win Conditions
Each day has a trust target and a limited clinic shift timer. The player clears the day by releasing enough animals with healthy or excellent recovery ratings before the shift ends. A day fails if too many animals leave stressed, if the waiting lane overflows, or if trust remains below target when time expires. Failed days are retryable without losing permanent upgrades.

## Progression Structure
Progression is day-based. Early days include cow and hen with single-condition treatment. Mid-game adds sheep, goat, piglet, rabbit, and two-condition animals. Later days add rare alpaca and pony visitors, urgent priority markers, limited medicine stock, and event modifiers such as cold morning, muddy pasture, festival crowd, or sleepy animals. Persistent upgrades include intake lane size, assistant auto-check, station speed, medicine stock, comfort bedding, and recovery meadow capacity.

## Level Or Run Structure
Each run is a 3 to 5 minute clinic shift. A shift contains arrival waves, care decisions, recovery timers, and a short result screen. Days vary by animal roster, condition mix, stock limits, and target trust. The ranch board remains stable enough to learn, but station demand and animal condition combinations change each day.

## Economy, Rewards, Drops, And Upgrades
Coins come from successful releases and bonus product gifts. Trust comes from fast, accurate, low-stress care. Product gifts include milk, eggs, wool, goat milk, truffles, and soft fur, but they are rewards rather than the main production loop. Medicine coupons unlock stronger treatment cards. Upgrades should create strategic choices: faster treatment, larger card hand, more waiting space, lower stress, or better rare visitor rewards.

## Gameplay Diversity And Content Budget
The genre family is casual management, with a concrete sub-archetype of veterinary triage and recovery routing. It is not a reskin of Meadow Snack Ranch because the primary decision is diagnosis and station sequencing instead of feed recipe matching. It is not Pasture Parade Deluxe because it avoids adjacency combo pen care and cleanliness chains as the central scoring system. It is not Market Morning Farm because there is no crop pricing, stall stocking, or customer queue.

The playfield is a three-quarter top-down pasture clinic board with at least eight interactive regions: arrival lane, diagnosis desk, feed tonic trough, grooming brush station, warm blanket stall, medicine shelf, recovery meadow, and release gate. Terrain and map elements must include meadow grass, canvas clinic awning, wooden station signs, hay mat floors, medicine crate, water basin, waiting fence, recovery flowers, release arch, and upgrade board.

The roster target includes six normal animal types, two rare visitor animals, six condition types, six treatment card families, four recovery ratings, and five feedback effects. The mechanic target includes condition inspection, animal dragging, treatment matching, station queueing, timer collection, stress management, waiting lane overflow, day modifiers, and permanent upgrades.

Forbidden template reuse: do not copy the feed recipe loop from Meadow Snack Ranch, do not copy Pasture Parade Deluxe adjacency combo maintenance, do not use a crop market or pricing loop, do not represent animals as circles or label-only icons, and do not use the same left cookbook plus right clipboard UI composition.

## Visual Identity Contract
The UI layout archetype is a cozy pasture clinic board with a bottom treatment card tray, a curved left-side intake lane, and a right-side station status rack. The HUD uses a top-center trust meter shaped like a heart-tag, an upper-left shift timer, and compact coin and stress counters. Navigation uses anchored sheets for station upgrades, pause, help, and results.

The palette signature is clean meadow green, warm clinic white, soft coral red, straw yellow, and medicine teal. The material language is canvas awnings, rounded wooden station signs, paper diagnosis tags, soft cloth treatment cards, and gentle shadows. Typography should be rounded, friendly, English-only, and highly readable.

Playfield safety is strict: animal paths, care station drop zones, release gate, and intake lane must be free from decorative overlays. Frames belong only in reserved panel zones and station headers.

The icon subject is a friendly realistic cow and rabbit beside a red-cross ranch medical kit and a heart-shaped recovery tag. The silhouette must show a cow muzzle and horns, rabbit ears, the kit handle, and a heart tag. It must not reuse the cow-with-feed-bowl icon from Meadow Snack Ranch, a generic barn icon, a market crate icon, or a plain medical cross without animals.

## Screen Map
- Menu: game title, start button, sound toggle, help button, and animated pasture clinic background.
- Gameplay HUD: pasture clinic board, bottom treatment cards, intake lane, care stations, trust meter, shift timer, stress indicator, and toast feedback.
- Diagnosis Sheet: animal portrait, condition icons, recommended treatment hints, stress level, and reward estimate.
- Upgrade Sheet: intake lane, station speed, medicine stock, assistant check, bedding comfort, and recovery meadow upgrades.
- Pause: resume, restart shift, sound toggle, and return to menu.
- Result: released animals, excellent recoveries, stressed misses, coins, trust, product gifts, and next day or retry.
- Help: one-page explanation of symptoms, treatments, station queues, stress, and release rewards.

## UI Direction
The recommended `ui_skin` is `skin_cartoon_light`. The final UI must be better than bare UI Kit scaffolding. It should look like a cheerful ranch clinic board, not like a generic farm panel. The bottom treatment card tray is the main action surface, the intake lane must visually curve into the clinic, and station headers should use physical signboards. HUD priorities are trust, time, waiting pressure, selected animal status, and treatment stock.

## Gameplay Art Direction
The camera perspective is three-quarter top-down. Animals must have realistic readable silhouettes: cow with long body, muzzle, horns, legs, and udder; hen with beak, comb, wing, tail, and feet; sheep with wool mass and visible face; goat with horns and beard; piglet with snout and short legs; rabbit with long ears and compact body. Rare alpaca and pony should have distinct neck, body, and leg profiles. Simple circles, capsules, and label-only animals are not acceptable for delivery-ready output.

Required visual roles are animals, arrival lane, diagnosis desk, treatment cards, care stations, medicine crate, recovery meadow, release gate, product gift icons, condition icons, stress effects, and trust effects. Primary animals need idle, worried, being-treated, recovering, happy, excellent, and stressed states. Moving animals should face their movement direction by directional frames, flipping, or grouped silhouette orientation. Recovery should show visible feedback such as relaxed posture, heart tag, sparkle, or clean coat.

Anchors should be at the lower center of each animal body. Hitboxes should cover the body and the station card area without requiring precise taps. Z-order should place ground and station floors first, animals above stations, treatment effects above animals, and UI overlays only in reserved safe zones.

If the shared library lacks suitable animal art, the gameplay art workflow must import or generate license-clear animal sprites through the approved repository art workflow before delivery. Prototype-only shapes are allowed only for internal testing, not for final delivery.

## Icon Direction
The icon should be cartoon-polished but grounded in real animal shapes: a cow head and rabbit ears beside a ranch medical kit with a heart recovery tag. The color direction is meadow green, clinic white, coral red, medicine teal, and straw yellow. The tone is caring, premium, and immediately readable as animal rescue rather than generic medicine.

## BGM Direction
Menu music should be gentle, warm, and reassuring with soft acoustic plucks, light mallets, and airy pads. Gameplay music should be slightly busier but still cozy, matching a clinic shift with light urgency. A high-pressure shift variation may add soft hand percussion and faster pizzicato. SFX should cover animal arrival, diagnosis tap, treatment card select, station drop, recovery chime, stress warning, product gift, release gate, upgrade, error, day clear, and day fail.

## Technical Implementation Notes
Implementation should be a single Android Java project under `games/ranch_vet_rescue` after confirmation. The game should use `com.android.boot.MainActivity`, a custom Java game view for the pasture clinic board, and Android View/XML overlays for menu, pause, help, upgrades, diagnosis, and result screens. The state model should include MENU, PLAYING, PAUSED, and GAME_OVER or RESULT.

Important systems to plan early are animal condition generation, treatment-card matching, station queues, stress accumulation, waiting lane overflow, day modifiers, reward calculation, persistent upgrades, and a runtime gameplay art map for animal roles, facing, anchors, hitboxes, z-order, states, and movement rules.

## Differentiation Note Against Registry
Compared with `meadow_snack_ranch`, this game centers on veterinary diagnosis, station routing, stress, and recovery ratings rather than feed recipe matching and product-order quality. Compared with `pasture_parade_deluxe`, it avoids adjacency combo pen maintenance. Compared with `market_morning_farm`, it removes crop pricing and customer serving. Compared with crop and flower farm entries, it centers on animal health triage and visible recovery.

## Confirmation Gate
This requirements trace is a draft. Project initialization must not begin until the user explicitly confirms these requirements.
