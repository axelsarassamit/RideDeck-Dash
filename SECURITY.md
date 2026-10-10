# Security

Do not post keys, passwords, location histories, private footage or full device dumps in public issues. Report sensitive issues privately through the repository owner's GitHub profile contact options.

Dash does not yet implement authenticated companion recording. Production recording must authenticate the actual caller UID, allowed package and signing certificate. The pure lease policy is not sufficient.

Release updates are user initiated and verified against the installed app's certificate. GitHub checksum files provide corruption detection; the installed signer check and Android installer provide signing identity enforcement.
