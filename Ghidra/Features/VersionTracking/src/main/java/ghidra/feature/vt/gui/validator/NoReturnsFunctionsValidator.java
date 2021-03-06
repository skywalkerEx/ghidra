/* ###
 * IP: GHIDRA
 * REVIEWED: YES
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
package ghidra.feature.vt.gui.validator;

import ghidra.feature.vt.api.main.VTSession;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.*;
import ghidra.util.task.TaskMonitor;
import docking.widgets.conditiontestpanel.ConditionResult;
import docking.widgets.conditiontestpanel.ConditionStatus;

public class NoReturnsFunctionsValidator extends VTPreconditionValidator {

	private static final String NAME = "Number Of No-Returns Functions Validator";

	public static final String DIFFERENCE_THRESHOLD =
		"Maximum percentage difference between number of no-return functions in each program";
	public static final float DIFFERENCE_THRESHOLD_DEFAULT = 0;

	public NoReturnsFunctionsValidator(Program sourceProgram, Program destinationProgram,
			VTSession existingResults) {
		super(sourceProgram, destinationProgram, existingResults);
	}

	@Override
	public ConditionResult doRun(TaskMonitor monitor) {
		float threshold = DIFFERENCE_THRESHOLD_DEFAULT;
		int numSource = checkNumNoReturnFunctions(sourceProgram, monitor);
		int numDest = checkNumNoReturnFunctions(destinationProgram, monitor);
		ConditionStatus status = ConditionStatus.Passed;
		StringBuilder warnings = new StringBuilder();
		if (!monitor.isCancelled()) {
			int diff = Math.abs(numSource - numDest);
			int max = Math.max(numSource, numDest);
			float percent = (float) diff / (float) max;
			if (percent > threshold) {
				status = ConditionStatus.Warning;
				warnings.append(sourceProgram.getDomainFile().getName() + " and " +
					destinationProgram.getDomainFile().getName() + " have " + numSource + " and " +
					numDest + " no-return functions respectively," + "\n");
				warnings.append("which is a " + format(percent) +
					" percent difference, greater than the threshold of " + format(threshold) +
					"\n");
			}
		}
		return new ConditionResult(status, warnings.toString());
	}

	private static String format(float percent) {
		return String.format("%.1f%%", percent * 100.0);
	}

	private int checkNumNoReturnFunctions(Program prog, TaskMonitor monitor) {
		FunctionIterator funcIter = prog.getFunctionManager().getFunctions(true);
		int numNoReturnFuncs = 0;

		monitor.setIndeterminate(true);
		while (funcIter.hasNext() && !monitor.isCancelled()) {
			monitor.incrementProgress(1);
			Function func = funcIter.next();
			Address address = func.getEntryPoint();
			Instruction inst = prog.getListing().getInstructionAt(address);

			//This check gets rid of Import Address Table "fake" functions
			if (inst != null) {
				if (func.hasNoReturn()) {
					numNoReturnFuncs++;
				}
			}
		}
		return numNoReturnFuncs;
	}

	@Override
	public String getDescription() {
		return "Makes sure the two programs have nearly the same number of no-return functions.";
	}

	@Override
	public String getName() {
		return NAME;
	}
}
