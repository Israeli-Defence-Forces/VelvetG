# Integration audit

Editable implementation: source-patch/src/me/movi/violet; profile persistence and translation overrides remain in source-patch/src/me/eldodebug/soar. The inherited client is binary-only.

Baseline restored and independently verified: SHA256 737b416bf9256e7f22d28768bb6f54e6819ad797301251065298e58827e5293e. RestoreBase additionally verifies all 2,306 historical entries.

## Editable systems

VioletEngine schedules independent hands and maintains CPS. VioletRuntime owns input hooks and session cleanup. VioletSettings registers stable native profile keys. VioletModule and VioletHud use ModManager and the existing event bus. AssistModule/AssistRuntime provide Auto Tool, Auto Soup and Inventory Move; AssistScreen, VioletScreen, VioletUi, VioletStyle and VioletControls supply the compact existing UI. Five mixins hook tick, key state, pending count, held-slot sync and movement. ProfileManager retains JSON persistence; Translator retains existing translations.

## Build and compatibility

BUILD.sh/BUILD.bat invoke the existing Python/PowerShell/Java source-patch pipeline. It restores the exact baseline, checks downloaded artifact hashes, prefixes obfuscated Minecraft symbols for javac, compiles added code for Java 8, strips prefixes and patches five inherited classes (four integration hooks plus the Scout frame repair). Keep this pipeline and the launcher installer. The installer uses LaunchWrapper and GlideTweaker, not a Forge @Mod entry point. Standalone Forge loading is not supplied or proven. A Forge/OptiFine live compatibility test is outstanding.

## Licensing

The baseline includes LICENSE.txt (MIT), META-INF/licenses/Velvet-MIT.txt (MIT, liwwyy/movi), Velvet-NOTICE.md, and Inter/Unifont notices. Preserve them. Orven has no project licence file or metadata licence declaration in the supplied archive; no direct source reuse is authorised by the available evidence. Orven targets Java 25 with JDK 27, Fabric/Ornithe and OneConfig. None of those APIs or dependencies will be transplanted. Existing embedded third-party notices must remain intact; a complete historical dependency licence inventory cannot be inferred from absent original source. New work should add no runtime dependencies.

## Overlap decisions

Retain the Violet scheduler, physical-priority input bridge and native profile identities. Improve count storage, advanced CPS presentation and optional in-memory diagnostics. Retain AutoTool harvesting/efficiency logic, add durability tie resolution and safe manual-slot cleanup. Retain AutoSoup ordinary controller use and existing instant/normal modes; improve pending inventory-state handling rather than importing Orven timing profiles. New BedWars/world visuals are separately registered, default-off modules. Existing combat modules are enhanced in place to avoid duplicate systems.

## Inherited module inventory

- me/eldodebug/soar/management/mods/impl/WeatherChangerMod.class
- me/eldodebug/soar/management/mods/impl/ChatMod.class
- me/eldodebug/soar/management/mods/impl/StopwatchMod.class
- me/eldodebug/soar/management/mods/impl/appleskin/FoodValues.class
- me/eldodebug/soar/management/mods/impl/appleskin/AppleSkinHelper.class
- me/eldodebug/soar/management/mods/impl/BowZoomMod.class
- me/eldodebug/soar/management/mods/impl/ChatTranslateMod.class
- me/eldodebug/soar/management/mods/impl/MechvibesMod.class
- me/eldodebug/soar/management/mods/impl/HorseStatsMod.class
- me/eldodebug/soar/management/mods/impl/HealthDisplayMod.class
- me/eldodebug/soar/management/mods/impl/ScoreboardMod.class
- me/eldodebug/soar/management/mods/impl/HypixelQuickPlayMod.class
- me/eldodebug/soar/management/mods/impl/SnapTapMod.class
- me/eldodebug/soar/management/mods/impl/WaveyCapesMod.class
- me/eldodebug/soar/management/mods/impl/rearview/RearviewCamera.class
- me/eldodebug/soar/management/mods/impl/rearview/RenderGlobalHelper.class
- me/eldodebug/soar/management/mods/impl/ImageDisplayMod.class
- me/eldodebug/soar/management/mods/impl/SoundModifierMod.class
- me/eldodebug/soar/management/mods/impl/FPSBoostMod.class
- me/eldodebug/soar/management/mods/impl/CPSDisplayMod.class
- me/eldodebug/soar/management/mods/impl/mechibes/SoundKey.class
- me/eldodebug/soar/management/mods/impl/TaplookMod.class
- me/eldodebug/soar/management/mods/impl/TabEditorMod.class
- me/eldodebug/soar/management/mods/impl/BlockInfoMod.class
- me/eldodebug/soar/management/mods/impl/SpeedometerMod.class
- me/eldodebug/soar/management/mods/impl/ScoutMod.class
- me/eldodebug/soar/management/mods/impl/PlayerCounterMod.class
- me/eldodebug/soar/management/mods/impl/ItemUseDelayFixMod.class
- me/eldodebug/soar/management/mods/impl/ModernHotbarMod.class
- me/eldodebug/soar/management/mods/impl/MinimalDamageShakeMod.class
- me/eldodebug/soar/management/mods/impl/SkinProtectMod.class
- me/eldodebug/soar/management/mods/impl/CoordsMod.class
- me/eldodebug/soar/management/mods/impl/GodbridgeAssistMod.class
- me/eldodebug/soar/management/mods/impl/FullbrightMod.class
- me/eldodebug/soar/management/mods/impl/ItemInfoMod.class
- me/eldodebug/soar/management/mods/impl/ClearWaterMod.class
- me/eldodebug/soar/management/mods/impl/GlintColorMod.class
- me/eldodebug/soar/management/mods/impl/ItemPhysicsMod.class
- me/eldodebug/soar/management/mods/impl/HypixelMod.class
- me/eldodebug/soar/management/mods/impl/NametagMod.class
- me/eldodebug/soar/management/mods/impl/PackDisplayMod.class
- me/eldodebug/soar/management/mods/impl/InternalSettingsMod.class
- me/eldodebug/soar/management/mods/impl/FovModifierMod.class
- me/eldodebug/soar/management/mods/impl/ClientSpooferMod.class
- me/eldodebug/soar/management/mods/impl/TargetIndicatorMod.class
- me/eldodebug/soar/management/mods/impl/ArrayListMod.class
- me/eldodebug/soar/management/mods/impl/FPSDisplayMod.class
- me/eldodebug/soar/management/mods/impl/subtitle/Subtitle.class
- me/eldodebug/soar/management/mods/impl/BorderlessFullscreenMod.class
- me/eldodebug/soar/management/mods/impl/TimeChangerMod.class
- me/eldodebug/soar/management/mods/impl/ClockMod.class
- me/eldodebug/soar/management/mods/impl/FemaleGenderMod.class
- me/eldodebug/soar/management/mods/impl/DynamicWidgetMod.class
- me/eldodebug/soar/management/mods/impl/HitColorMod.class
- me/eldodebug/soar/management/mods/impl/AppleSkinMod.class
- me/eldodebug/soar/management/mods/impl/ShinyPotsMod.class
- me/eldodebug/soar/management/mods/impl/skin3d/layers/BodyLayerFeatureRenderer.class
- me/eldodebug/soar/management/mods/impl/skin3d/layers/HeadLayerFeatureRenderer.class
- me/eldodebug/soar/management/mods/impl/skin3d/SkinDirection.class
- me/eldodebug/soar/management/mods/impl/skin3d/opengl/NativeImage.class
- me/eldodebug/soar/management/mods/impl/skin3d/render/CustomizableCube.class
- me/eldodebug/soar/management/mods/impl/skin3d/render/CustomizableCubeListBuilder.class
- me/eldodebug/soar/management/mods/impl/skin3d/render/SolidPixelWrapper.class
- me/eldodebug/soar/management/mods/impl/skin3d/render/CustomizableModelPart.class
- me/eldodebug/soar/management/mods/impl/ColorSaturationMod.class
- me/eldodebug/soar/management/mods/impl/ReachCirclesMod.class
- me/eldodebug/soar/management/mods/impl/PlayerDisplayMod.class
- me/eldodebug/soar/management/mods/impl/DamageParticlesMod.class
- me/eldodebug/soar/management/mods/impl/WaypointMod.class
- me/eldodebug/soar/management/mods/impl/FPSLimiterMod.class
- me/eldodebug/soar/management/mods/impl/NameProtectMod.class
- me/eldodebug/soar/management/mods/impl/PotionCounterMod.class
- me/eldodebug/soar/management/mods/impl/NameDisplayMod.class
- me/eldodebug/soar/management/mods/impl/PlayTimeDisplayMod.class
- me/eldodebug/soar/management/mods/impl/SlowSwingMod.class
- me/eldodebug/soar/management/mods/impl/MotionBlurMod.class
- me/eldodebug/soar/management/mods/impl/MinimapMod.class
- me/eldodebug/soar/management/mods/impl/UHCOverlayMod.class
- me/eldodebug/soar/management/mods/impl/NoJumpDelayMod.class
- me/eldodebug/soar/management/mods/impl/DamageTiltMod.class
- me/eldodebug/soar/management/mods/impl/KeystrokesMod.class
- me/eldodebug/soar/management/mods/impl/AsyncScreenshotMod.class
- me/eldodebug/soar/management/mods/impl/EntityCullingMod.class
- me/eldodebug/soar/management/mods/impl/AnimationsMod.class
- me/eldodebug/soar/management/mods/impl/KillSoundsMod.class
- me/eldodebug/soar/management/mods/impl/waveycapes/layers/CustomCapeRenderLayer.class
- me/eldodebug/soar/management/mods/impl/waveycapes/layers/SmoothCapeRenderer.class
- me/eldodebug/soar/management/mods/impl/waveycapes/sim/StickSimulation.class
- me/eldodebug/soar/management/mods/impl/ReachDisplayMod.class
- me/eldodebug/soar/management/mods/impl/InventoryMod.class
- me/eldodebug/soar/management/mods/impl/minimap/ChunkTile.class
- me/eldodebug/soar/management/mods/impl/minimap/ChunkAtlas.class
- me/eldodebug/soar/management/mods/impl/CompassMod.class
- me/eldodebug/soar/management/mods/impl/ProjectileTrailMod.class
- me/eldodebug/soar/management/mods/impl/TargetInfoMod.class
- me/eldodebug/soar/management/mods/impl/dynamicwidget/MiniJson.class
- me/eldodebug/soar/management/mods/impl/dynamicwidget/SpotifyLyricsService.class
- me/eldodebug/soar/management/mods/impl/dynamicwidget/WindowsMediaSessionBridge.class
- me/eldodebug/soar/management/mods/impl/ServerIPDisplayMod.class
- me/eldodebug/soar/management/mods/impl/QuickSwitchMod.class
- me/eldodebug/soar/management/mods/impl/CrosshairMod.class
- me/eldodebug/soar/management/mods/impl/InventoryDisplayMod.class
- me/eldodebug/soar/management/mods/impl/DayCounterMod.class
- me/eldodebug/soar/management/mods/impl/ZoomMod.class
- me/eldodebug/soar/management/mods/impl/Items2DMod.class
- me/eldodebug/soar/management/mods/impl/hypixel/HypixelGameMode.class
- me/eldodebug/soar/management/mods/impl/ChunkAnimatorMod.class
- me/eldodebug/soar/management/mods/impl/PingDisplayMod.class
- me/eldodebug/soar/management/mods/impl/SessionInfoMod.class
- me/eldodebug/soar/management/mods/impl/ChunkBordersMod.class
- me/eldodebug/soar/management/mods/impl/ToggleSprintMod.class
- me/eldodebug/soar/management/mods/impl/Skin3DMod.class
- me/eldodebug/soar/management/mods/impl/ToggleSneakMod.class
- me/eldodebug/soar/management/mods/impl/DiscordRPCMod.class
- me/eldodebug/soar/management/mods/impl/JumpCircleMod.class
- me/eldodebug/soar/management/mods/impl/MoBendsMod.class
- me/eldodebug/soar/management/mods/impl/TNTTimerMod.class
- me/eldodebug/soar/management/mods/impl/RearviewMod.class
- me/eldodebug/soar/management/mods/impl/ArmorStatusMod.class
- me/eldodebug/soar/management/mods/impl/OverlayEditorMod.class
- me/eldodebug/soar/management/mods/impl/CustomHeldItemsMod.class
- me/eldodebug/soar/management/mods/impl/RawInputMod.class
- me/eldodebug/soar/management/mods/impl/ParticleCustomizerMod.class
- me/eldodebug/soar/management/mods/impl/PotionStatusMod.class
- me/eldodebug/soar/management/mods/impl/KillEffectsMod.class
- me/eldodebug/soar/management/mods/impl/DamageTintMod.class
- me/eldodebug/soar/management/mods/impl/EarsMod.class
- me/eldodebug/soar/management/mods/impl/CalendarMod.class
- me/eldodebug/soar/management/mods/impl/MouseStrokesMod.class
- me/eldodebug/soar/management/mods/impl/BlockOverlayMod.class
- me/eldodebug/soar/management/mods/impl/FarCameraMod.class
- me/eldodebug/soar/management/mods/impl/WeatherDisplayMod.class
- me/eldodebug/soar/management/mods/impl/BloodParticlesMod.class
- me/eldodebug/soar/management/mods/impl/AutoTextMod.class
- me/eldodebug/soar/management/mods/impl/asyncscreenshot/AsyncScreenshots.class
- me/eldodebug/soar/management/mods/impl/PlayerPredicatorMod.class
- me/eldodebug/soar/management/mods/impl/BossHealthMod.class
- me/eldodebug/soar/management/mods/impl/MinemenMod.class
- me/eldodebug/soar/management/mods/impl/ClearGlassMod.class
- me/eldodebug/soar/management/mods/impl/MemoryUsageMod.class
- me/eldodebug/soar/management/mods/impl/FPSSpooferMod.class
- me/eldodebug/soar/management/mods/impl/ComboCounterMod.class
- me/eldodebug/soar/management/mods/impl/SoundSubtitlesMod.class
- me/eldodebug/soar/management/mods/impl/BreadcrumbsMod.class
- me/eldodebug/soar/management/mods/impl/projectiletrail/ProjectileTrailType.class
- me/eldodebug/soar/management/mods/impl/ThickRodsMod.class
- me/eldodebug/soar/management/mods/impl/PlayerListMod.class
- me/eldodebug/soar/management/mods/impl/HitBoxMod.class
- me/eldodebug/soar/management/mods/impl/MinimalViewBobbingMod.class
- me/eldodebug/soar/management/mods/impl/FreelookMod.class
- me/eldodebug/soar/management/mods/impl/NoBreakDelayMod.class
- me/eldodebug/soar/management/mods/impl/HitDelayFixMod.class

## Inherited event inventory

- me/eldodebug/soar/management/event/ArrayHelper$1.class
- me/eldodebug/soar/management/event/Event.class
- me/eldodebug/soar/management/event/EventManager$1.class
- me/eldodebug/soar/management/event/ArrayHelper.class
- me/eldodebug/soar/management/event/Priority.class
- me/eldodebug/soar/management/event/Data.class
- me/eldodebug/soar/management/event/EventTarget.class
- me/eldodebug/soar/management/event/EventManager.class
- me/eldodebug/soar/management/event/impl/EventRender2D.class
- me/eldodebug/soar/management/event/impl/EventTick.class
- me/eldodebug/soar/management/event/impl/EventUpdateFramebufferSize.class
- me/eldodebug/soar/management/event/impl/EventRenderDamageTint.class
- me/eldodebug/soar/management/event/impl/EventSendPacket.class
- me/eldodebug/soar/management/event/impl/EventLeaveServer.class
- me/eldodebug/soar/management/event/impl/EventJoinServer.class
- me/eldodebug/soar/management/event/impl/EventRendererLivingEntity.class
- me/eldodebug/soar/management/event/impl/EventDamageEntity.class
- me/eldodebug/soar/management/event/impl/EventLivingUpdate.class
- me/eldodebug/soar/management/event/impl/EventRenderItemInFirstPerson.class
- me/eldodebug/soar/management/event/impl/EventLoadWorld.class
- me/eldodebug/soar/management/event/impl/EventRenderVisualizer.class
- me/eldodebug/soar/management/event/impl/EventJump.class
- me/eldodebug/soar/management/event/impl/EventCameraRotation.class
- me/eldodebug/soar/management/event/impl/EventHitOverlay.class
- me/eldodebug/soar/management/event/impl/EventText.class
- me/eldodebug/soar/management/event/impl/EventRenderPlayerStats.class
- me/eldodebug/soar/management/event/impl/EventScrollMouse.class
- me/eldodebug/soar/management/event/impl/EventSwitchTexture.class
- me/eldodebug/soar/management/event/impl/EventRenderExpBar.class
- me/eldodebug/soar/management/event/impl/EventKey.class
- me/eldodebug/soar/management/event/impl/EventMotionUpdate.class
- me/eldodebug/soar/management/event/impl/EventUpdate.class
- me/eldodebug/soar/management/event/impl/EventToggleFullscreen.class
- me/eldodebug/soar/management/event/impl/EventLocationCape.class
- me/eldodebug/soar/management/event/impl/EventZoomFov.class
- me/eldodebug/soar/management/event/impl/EventClickMouse.class
- me/eldodebug/soar/management/event/impl/EventRenderHitbox.class
- me/eldodebug/soar/management/event/impl/EventUpdateDisplay.class
- me/eldodebug/soar/management/event/impl/EventRenderNotification.class
- me/eldodebug/soar/management/event/impl/EventSendChat.class
- me/eldodebug/soar/management/event/impl/EventPreRenderTick.class
- me/eldodebug/soar/management/event/impl/EventRenderScoreboard.class
- me/eldodebug/soar/management/event/impl/EventRenderChunkPosition.class
- me/eldodebug/soar/management/event/impl/EventPlaySound.class
- me/eldodebug/soar/management/event/impl/EventHurtCamera.class
- me/eldodebug/soar/management/event/impl/EventRenderTick.class
- me/eldodebug/soar/management/event/impl/EventRenderPumpkinOverlay.class
- me/eldodebug/soar/management/event/impl/EventLocationSkin.class
- me/eldodebug/soar/management/event/impl/EventBlockHighlightRender.class
- me/eldodebug/soar/management/event/impl/EventPreRenderChunk.class
- me/eldodebug/soar/management/event/impl/EventRenderCrosshair.class
- me/eldodebug/soar/management/event/impl/EventRenderSelectedItem.class
- me/eldodebug/soar/management/event/impl/EventGamma.class
- me/eldodebug/soar/management/event/impl/EventRenderPlayer.class
- me/eldodebug/soar/management/event/impl/EventAttackEntity.class
- me/eldodebug/soar/management/event/impl/EventFovUpdate.class
- me/eldodebug/soar/management/event/impl/EventRenderTNT.class
- me/eldodebug/soar/management/event/impl/EventRender3D.class
- me/eldodebug/soar/management/event/impl/EventShader.class
- me/eldodebug/soar/management/event/impl/EventReceivePacket.class
- me/eldodebug/soar/management/event/impl/EventWaterOverlay.class
- me/eldodebug/soar/management/event/impl/EventRenderTooltip.class
- me/eldodebug/soar/management/event/impl/EventFireOverlay.class
- me/eldodebug/soar/management/event/impl/EventPlayerHeadRotation.class

## Mixin configurations

- mixins.soar.json
- mixins.soar.refmap.json
- mixins.violet.json

## Inherited Scout verification defect

The crash report exposed an invalid multi-catch StackMapTable in the baseline PikaApiClient class. Byte preservation alone did not establish JVM validity. The pipeline now repairs only that class’s frames and JVM-loads all 280 inherited Scout/module implementation classes without initializing the game; the regression suite checks that Scout code/members remain unchanged. The original and repaired outputs were tested on an actual Java 8 runtime.
