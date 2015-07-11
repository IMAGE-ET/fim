/*
 * This file is part of Fim - File Integrity Manager
 *
 * Copyright (C) 2015  Etienne Vrignaud
 *
 * Fim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Fim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fim.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fim.command;

import static org.fim.util.FormatUtil.formatDate;

import java.io.File;

import org.fim.internal.StateManager;
import org.fim.model.FileState;
import org.fim.model.Parameters;
import org.fim.model.State;

public class ResetDatesCommand extends AbstractCommand
{
	@Override
	public String getCmdName()
	{
		return "reset-dates";
	}

	@Override
	public String getShortCmdName()
	{
		return "rdates";
	}

	@Override
	public String getDescription()
	{
		return "Reset the file modification dates like it's stored in the last committed State";
	}

	@Override
	public void execute(Parameters parameters) throws Exception
	{
		fastCompareNotSupported(parameters);

		StateManager manager = new StateManager(parameters);
		State lastState = manager.loadLastState();

		System.out.println("Reset file modification dates based on the last committed State done " + formatDate(lastState.getTimestamp()));
		if (lastState.getMessage().length() > 0)
		{
			System.out.println("Message: " + lastState.getMessage());
		}
		System.out.println("");

		int dateResetCount = 0;
		for (FileState fileState : lastState.getFileStates())
		{
			File file = new File(fileState.getFileName());
			if (file.exists())
			{
				long lastModified = file.lastModified();
				if (lastModified != fileState.getLastModified())
				{
					dateResetCount++;
					file.setLastModified(fileState.getLastModified());
					System.out.printf("Set file modification: %s\t%s -> %s%n", fileState.getFileName(),
							formatDate(lastModified), formatDate(fileState.getLastModified()));
				}
			}
		}

		if (dateResetCount == 0)
		{
			System.out.printf("No file modification date have been reset%n");
		}
		else
		{
			System.out.printf("%d file modification dates have been reset%n", dateResetCount);
		}
	}
}