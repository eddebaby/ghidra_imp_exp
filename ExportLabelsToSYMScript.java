/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// List function names, entry point addresses, and code length to a SYM file
// Designed to work with Game Boy banked ROMs (banks are in Overlays)
// Will need fine-tuning, especially for other platforms

// Ripped off from: Ghidra's ExportFunctionInfoScript.java

// TODO: add .data and cribbing of data blocks
// TODO: ?? add .text and .image blocks? (Might need to patch Game Boy plugin too to add data types?)
// TODO: save with .sym file extension by default/ option
// TODO: add GUI function for adding length/section type
// ensure final file is ordered by address, not by code/data etc (un-important?)
// TODO!!!: add labels "like .loop" too (only doing functions and data, these are just labels). 

//@author Ed Kearney
//@category _NEW_
//@keybinding 
//@menupath 
//@toolbar 

import ghidra.app.plugin.core.script.Ingredient;
import ghidra.app.plugin.core.script.IngredientDescription;
import ghidra.app.script.GatherParamPanel;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;

import ghidra.util.filechooser.ExtensionFileFilter;
import docking.DialogComponentProvider;
import docking.widgets.filechooser.GhidraFileChooser;

import java.io.*;
import java.nio.charset.Charset;

public class ExportLabelsToSYMScript extends GhidraScript implements Ingredient {

	@Override
	public void run() throws Exception {
		//GhidraFileChooser chooser = new GhidraFileChooser(null);
		//ExtensionFileFilter filter = new ExtensionFileFilter("sym", "Symbol File");
		//chooser.addFileFilter(filter);
		IngredientDescription[] ingredients = getIngredientDescriptions();
		for (int i = 0; i < ingredients.length; i++) {
			state.addParameter(ingredients[i].getID(), ingredients[i].getLabel(),
				ingredients[i].getType(), ingredients[i].getDefaultValue());
		}
		//state.gath
		if (!state.displayParameterGatherer("Save SYM File")) {
			return;
		}
		
		File outputNameFile = (File) state.getEnvironmentVar("FunctionNameOutputFile");
		PrintWriter pWriter = new PrintWriter(new FileOutputStream(outputNameFile));
		Boolean lengthFlag = false;
		try {
			Integer.parseInt(state.getEnvironmentVar("FunctionStoreLengthFlag").toString());
			lengthFlag = Integer.parseInt(state.getEnvironmentVar("FunctionStoreLengthFlag").toString()) != 0;
			} catch (NumberFormatException e) {printerr("NumberFormatException");}
		Boolean commentFlag = false;
		try {
			commentFlag = Integer.parseInt(state.getEnvironmentVar("FunctionStoreCommentFlag").toString()) != 0;
			} catch (NumberFormatException nfe) {}
		Boolean junkFlag = false;
		try {
			junkFlag = Integer.parseInt(state.getEnvironmentVar("FunctionStoreJunkFlag").toString()) != 0;
			} catch (NumberFormatException nfe) {}
		Listing listing = currentProgram.getListing();
		pWriter.println(";Functions");
		FunctionIterator iter = listing.getFunctions(true);
		//	getData(true)
		while (iter.hasNext() && !monitor.isCancelled()) {
			Function f = iter.next();
			String fName = f.getName();
			Address entry = f.getEntryPoint();
			String fLength = Long.toHexString(f.getBody().getNumAddresses()).toString();
			if (entry == null) {
				//pWriter.println("/* FUNCTION_NAME_ " + fName + " FUNCTION_ADDR_ " +
					//"NO_ENTRY_POINT" + " */");
				println("WARNING: no entry point for " + fName);
			}
			else {
				//pWriter.println("/* FUNCTION_NAME_ " + fName + " FUNCTION_ADDR_ " + entry + " */");
				//String bankedAddress = f.getBody().toString(); //.replace("rom","");
				//bankedAddress = String.format("%02d", bankedAddress) + ":" + entry.toString();
				String bankedAddress = entry.toString();
				String bankNum = "00";
				if (bankedAddress.contains(":")) {
					bankedAddress = bankedAddress.replace("rom","0").replace("vram","0").replace("xram","0").replace("wram","0").replace("oam","0").replace("io","0").replace("hram","0").replace("ie","0");
					bankNum = bankedAddress.substring(0,bankedAddress.indexOf(":"));
					
					bankedAddress = bankedAddress.substring(bankedAddress.indexOf(":") + 2);
					//bankNum = bankNum.substring(0, bankNum.indexOf(":"));
					bankNum = String.format("%02d", Integer.parseInt(bankNum));
				}
				pWriter.println(bankNum + ":" + bankedAddress + " " + fName + (commentFlag ? (listing.getComment(0, entry) != null ? " " + (listing.getComment(0, entry).indexOf(";") != 0 ? ";" : "") + listing.getComment(0, entry) : "") : "" ));
				if (lengthFlag) pWriter.println(bankNum + ":" + bankedAddress + " " + ".code:" + fLength);
			}
		}
		pWriter.println(";Data");
		DataIterator dIter = listing.getDefinedData(true);
		//dIter = listing.get
		while (dIter.hasNext() && !monitor.isCancelled()) {
			Data d = dIter.next();
			String dName = d.getLabel();
			Address start = d.getAddress();
			String dLength = Long.toHexString(d.getLength()).toString();
			if (start == null) {
				println("WARNING: no address for " + dName); //will never happen with data?
			}
			else {
				String bankedAddress = start.toString();
				String bankNum = "00";
				if (bankedAddress.contains(":")) {
					bankedAddress = bankedAddress.replace("rom","0").replace("vram","0").replace("xram","0").replace("wram","0").replace("oam","0").replace("io","0").replace("hram","0").replace("ie","0");
					bankNum = bankedAddress.substring(0,bankedAddress.indexOf(":"));
					
					bankedAddress = bankedAddress.substring(bankedAddress.indexOf(":") + 2);
					//bankNum = bankNum.substring(0, bankNum.indexOf(":"));
					bankNum = String.format("%02d", Integer.parseInt(bankNum));
				}
				if (dName == null) dName = "null";
				if ((dName != null) ? !(dName.toLowerCase() == "null" && junkFlag) : false) {
					pWriter.println(bankNum + ":" + bankedAddress + " " + dName + (commentFlag ? (listing.getComment(0, start) != null && (listing.getComment(0, start).toLowerCase() != "null" && junkFlag) ? " " + (listing.getComment(0, start).indexOf(";") != 0 ? ";" : "")  + listing.getComment(0, start) : "") : "" ));
					if (lengthFlag) pWriter.println(bankNum + ":" + bankedAddress + " " + ".data:" + dLength);
				}
			}
		}
		pWriter.close();
	}

	@Override
	public IngredientDescription[] getIngredientDescriptions() {
		IngredientDescription[] retVal =
			new IngredientDescription[] { new IngredientDescription("FunctionNameOutputFile",
				"Output Symbol File (.sym):", GatherParamPanel.FILE, new String ("D:\\Emulation\\GB\\Games\\Pok\u00E9mon Card GB 2 - Here Comes Team Great Rocket (English).sym".getBytes(), Charset.forName("UTF-8"))), new IngredientDescription("FunctionStoreLengthFlag",
						"Include \'mgbdis\' section type and length?", GatherParamPanel.INTEGER, "1"), new IngredientDescription("FunctionStoreCommentFlag",
								"Include EOL comments?", GatherParamPanel.INTEGER, "1"), new IngredientDescription("FunctionStoreJunkFlag",
										"Exclude NULL data and comments?", GatherParamPanel.INTEGER, "1") } ;
		return retVal;
	}

}
