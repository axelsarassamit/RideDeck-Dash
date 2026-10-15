RideDeck Dash 0.8.4

Recording now has a five-minute charger grace period. It may continue without a charger while battery is at least 30%, then stops and finalizes the current clips when the allowance expires. Below 30% or unknown battery stops unplugged recording immediately. Reconnecting a charger resets the allowance. Movement and missing GPS do not extend it.

This replaces the previous unplugged-and-stationary stop setting. The timer is shared across recording restarts, activity rotation and app-process restarts within the same boot. When Dash first observes an unplugged phone, its allowance begins then. Plugging in alone does not automatically restart recording. The recording information menu shows remaining grace seconds.

Export, backup, editing and sound-check operations keep their existing charging-or-50% reserve. Background recording still requires an active authenticated RideDeck or RideDeck for Yamaha ride session. Existing clips and saved recording settings are preserved.

Local policy tests cover the exact five-minute boundary, 29%/30% battery boundary, reconnection, timer restoration and background authorization. The full physical five-minute disconnect and low-battery cutoff still need ride validation. No video stabilization changes are included.
