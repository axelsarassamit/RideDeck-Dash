# RideDeck Dash branding

Current identity is the approved Velocity R / Roadline design documented in ROADLINE_REFERENCE.md. The original transparent Canva Dash artwork is app/src/main/res/drawable-nodpi/velocity_logo.png. Preserve its actual R/road shape, white-and-amber ink and original RideDeck Dash lettering. The launcher uses the existing native vector adaptation in velocity_launcher_foreground.xml.

The cockpit and video watermark load that same original PNG through VelocityArtwork. The compact watermark arranges crops of the original mark and lettering horizontally without redrawing them or substituting a system font. Keep its bitmap shared so a multi-clip composition does not decode a full logo for every source. It is mandatory, drawn once after the composed video, and remains above PiP and text. Panel opacity and placement are editable; brand artwork itself retains its original colours.

The previous blue outline watermark has been removed. ic_dash.xml is a legacy Android notification small-icon resource, not the current brand master or exported watermark. Android renders small notification icons as a monochrome mask.
