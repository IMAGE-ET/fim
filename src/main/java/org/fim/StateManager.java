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
package org.fim;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.fim.util.FormatUtil.formatDate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.fim.model.CompareMode;
import org.fim.model.FileState;
import org.fim.model.State;

public class StateManager
{
	public static final String LAST_STATE_FILE_NAME = "lastState";

	private final File stateDir;
	private final CompareMode compareMode;
	private final Charset utf8 = Charset.forName("UTF-8");
	private int lastStateNumber = -1;

	public StateManager(File stateDir, CompareMode compareMode)
	{
		this.stateDir = stateDir;
		this.compareMode = compareMode;
	}

	public void createNewState(State state) throws IOException
	{
		state.saveToZipFile(getNextStateFile());
		writeLastStateNumber();
	}

	public File getNextStateFile()
	{
		findLastStateNumber();
		lastStateNumber++;
		File statFile = getStateFile(lastStateNumber);
		return statFile;
	}

	public State loadLastState() throws IOException
	{
		findLastStateNumber();
		return loadState(lastStateNumber);
	}

	public State loadState(int stateNumber) throws IOException
	{
		File stateFile = getStateFile(stateNumber);
		if (!stateFile.exists())
		{
			throw new IllegalStateException(String.format("Unable to load State file %d from directory %s", stateNumber, stateDir));
		}

		State state = new State();
		state.loadFromZipFile(stateFile);

		if (compareMode == CompareMode.FAST)
		{
			// Replace the real file hash by 'no_hash' to be able to compare the FileState entry
			for (FileState fileState : state.getFileStates())
			{
				fileState.setHash(StateGenerator.NO_HASH);
			}
		}

		return state;
	}

	private File getStateFile(int stateNumber)
	{
		return new File(stateDir, "state_" + stateNumber + ".zjson");
	}

	private void findLastStateNumber()
	{
		readLastStateNumber();
		if (lastStateNumber != -1)
		{
			return;
		}

		for (int index = 1; ; index++)
		{
			File statFile = getStateFile(index);
			if (!statFile.exists())
			{
				lastStateNumber = index - 1;
				return;
			}
		}
	}

	private void readLastStateNumber()
	{
		lastStateNumber = -1;

		File lastStateFile = new File(stateDir, LAST_STATE_FILE_NAME);
		if (lastStateFile.exists())
		{
			try
			{
				List<String> strings = Files.readAllLines(lastStateFile.toPath(), utf8);
				if (strings.size() > 0)
				{
					lastStateNumber = Integer.parseInt(strings.get(0));
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private void writeLastStateNumber()
	{
		if (lastStateNumber != -1)
		{
			File lastStateFile = new File(stateDir, LAST_STATE_FILE_NAME);
			String content = "" + lastStateNumber;
			try
			{
				Files.write(lastStateFile.toPath(), content.getBytes(), CREATE);
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}

	public void resetDates(State state)
	{
		System.out.println("Reset file modification dates based on the last committed State done " + formatDate(state.getTimestamp()));
		if (state.getMessage().length() > 0)
		{
			System.out.println("Message: " + state.getMessage());
		}
		System.out.println("");

		int dateResetCount = 0;
		for (FileState fileState : state.getFileStates())
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

	public void displayStatesLog() throws IOException
	{
		readLastStateNumber();
		if (lastStateNumber == -1)
		{
			System.out.println("No State found");
			return;
		}

		for (int stateNumber = 1; stateNumber <= lastStateNumber; stateNumber++)
		{
			File statFile = getStateFile(stateNumber);
			if (statFile.exists())
			{
				State state = loadState(stateNumber);
				System.out.printf("State #%d: %s%n", stateNumber, formatDate(state.getTimestamp()));
				if (state.getMessage().length() > 0)
				{
					System.out.printf("\tMessage: %s%n", state.getMessage());
				}
				System.out.printf("\tContains %d files%n", state.getFileCount());
				System.out.println("");
			}
		}
	}
}