# Social video editor

## Open and assemble

In Recordings, use **Edit video** on one recording, or **Select clips** then **Social export / merge selected**. Both open the same timeline editor. The former single-clip and overlay-export editors have been removed.

The large preview shows the composed output. Portrait places the timeline and tools below it; landscape places the timeline and tools beside it. Swipe the bottom tool strip for Clips, Crop, Front camera, Overlays, Text, Sound, Adjust and Stabilize.

Tap a thumbnail strip to choose a clip and seek. Drag its amber trim handles, or use Trim for precise decimal-second boundaries. Split at the playhead, duplicate, move left/right, or remove a section. Removal only changes the edit. Undo/redo keeps up to 30 steps during the current editor instance. Up to 30 sections and 30 minutes of selected output are supported. Split and duplicate can reference the same original safely.

## Picture and overlays

- Crop: 9:16 for TikTok/Reels/Stories, 4:5 feed, square 1:1, or landscape 16:9. Fill or retain the whole picture; zoom and pan with sliders or drag framing on the preview after zooming. Safe-area guides are editor-only visual aids, not platform guarantees.
- Front camera: the rear is the main picture and the front is a small, rounded inset. Change its size, snap to a corner, or drag its position. Front timestamps must come from finalized same-ride footage. Missing intervals hide PiP. Offset adjustment supports -5000 to 5000 milliseconds; capture-start matching is not frame-accurate hardware synchronization. Front audio is muted.
- Overlays: logo-only, speed, ride/map, all data or custom selection. Position and size the RideDeck mark, speed/data group, clock and route map separately. Adjust panel opacity, accent colour, speed units, map follow/overview and coverage. Branding is mandatory and drawn last. The map uses recorded GPS with a moving bike and offline trace; this editor does not download road tiles. Unrecorded data remains unavailable.
- Text: one multi-line title/caption, up to 200 characters, with output start/end times, position, size, font, colour and optional dark background. It stays on the project output timeline across cuts. Batch exports restart that timeline for each separate output.
- Adjust: brightness, contrast, saturation, Natural/Clear road/Soft/Black & white looks.

Preview/export share the same framing, colour, audio and overlay recipe. Native portrait front footage retains its shape in the PiP.

## Sound and stabilization

Recorded sound has volume and mute controls. Add a local audio document as music, set its volume, remove it or replace it. Music begins at project time zero and stops at its file end or project end. The music document permission is retained, but deleting/moving the source can make it unavailable. The original and music are mixed; there are no separately captured voice/road stems. Optional 0.5-second fades apply at each main clip edge and the music's ends. Wind-rumble reduction is a low-frequency filter, not speech isolation. Preview and export use these processors.

Stabilize offers Off, Gentle (12% zoom), Strong (22%) and Maximum (35%). It analyzes each selected main-clip trim before preparing the playable preview. Export uses the same correction; PiP is unaffected. Analyses are cached in bounded memory for reuse, not permanently stored as a project requirement. Analysis restarts if necessary after process death. Feature-poor footage can fail analysis; turn stabilization off or select a different section. It remains experimental and cannot reliably reconstruct missing frames, severe blur or rolling-shutter distortion. No new road-quality claim follows from synthetic tests.

## Saved edits and export

Edits auto-save in private app storage. Use Drafts / Save as new draft for another copy, and reopen from Saved video edits in Recordings. Order, trims, PiP and the complete editor recipe survive closing and recreation. Undo history is not persisted. Drafts reference original recordings and music; they are not backups and cannot restore a source removed by the user or loop recycling.

Export one merged video or separate social copies. Choose 720p/4 Mbps, 1080p/8 Mbps or 1080p/16 Mbps; the export menu estimates video size. Outputs use H.264 MP4 with AAC audio, with output cadence capped at 30 fps. There is no automatic posting. Save/share the finished video, then add platform music, descriptions and publish in the social app.

Copies go to Movies/RideDeckDash/Social. Leaving, rotating, starting recording or losing the charger-or-50% editing reserve cancels the unfinished export. Completed batch copies remain saved. Originals and recording settings are preserved. Recording itself retains its separate five-minute unplugged grace and 30% minimum.

## Recording library

Recordings show small video thumbnails. Sort by newest, oldest, name, largest or smallest. Filter all/rear/front/protected. Sort/filter preferences survive reopening. Select visible adds the visible set; hidden selected clips remain selected and count toward export/deletion. Clear selection clears the whole selection. Thumbnail decoding pauses during recording/finalization to protect capture resources.

## App presets

Existing social defaults and the three named saved app-settings slots remain in Setup. These are separate from per-video drafts. Loading an app-settings slot changes recording/audio/storage/automation and social defaults only after confirmation; footage and companion authentication are not part of a preset. Existing projects keep their frozen editor recipe.
