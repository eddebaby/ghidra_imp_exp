# ghidra_imp_exp
Ghidra scripts to import and export information to/from Ghidra projects.

Scripts:

ExportLabelsToSYMScript.java :-

- Ripped off from: Ghidra's ExportFunctionInfoScript.java
- "Only just working" version - needs cleaning up and improving.
- Exports labels from a Ghidra project in to a .sym file
- This is intended for use with BGB, helping to debug Gameboy and Gameboy Color ROMs - but it essentially follows the NO$GBA .sym file format.
