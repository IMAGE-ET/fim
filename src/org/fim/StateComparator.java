/*
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fim;

import static org.fim.util.FormatUtil.formatDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fim.model.Difference;
import org.fim.model.FileState;
import org.fim.model.State;

/**
 * @author evrignaud
 */
public class StateComparator
{
	private final CompareMode compareMode;

	private List<Difference> added;
	private List<Difference> duplicated;
	private List<Difference> dateModified;
	private List<Difference> contentModified;
	private List<Difference> renamed;
	private List<Difference> deleted;

	public StateComparator(CompareMode compareMode)
	{
		this.compareMode = compareMode;
	}

	public StateComparator compare(State previousState, State currentState)
	{
		List<FileState> previousFileStates = new ArrayList<>();
		List<FileState> differences = new ArrayList<>();
		List<FileState> addedOrModified = new ArrayList<>();

		if (previousState != null)
		{
			System.out.println("Comparing with previous state from " + formatDate(previousState.getTimestamp()));
			if (previousState.getMessage().length() > 0)
			{
				System.out.println("Message: " + previousState.getMessage());
			}
			System.out.println("");

			previousFileStates.addAll(previousState.getFileStates());
		}

		differences.addAll(previousFileStates);

		for (FileState fileState : currentState.getFileStates())
		{
			if (!differences.remove(fileState))
			{
				addedOrModified.add(fileState);
			}
		}

		added = new ArrayList<>();
		duplicated = new ArrayList<>();
		dateModified = new ArrayList<>();
		contentModified = new ArrayList<>();
		renamed = new ArrayList<>();
		deleted = new ArrayList<>();

		int sameFileNameIndex;
		List<FileState> samePreviousHash;
		for (FileState fileState : addedOrModified)
		{
			if ((sameFileNameIndex = findSameFileName(fileState, differences)) != -1)
			{
				FileState previousFileState = differences.remove(sameFileNameIndex);
				if (previousFileState.getHash().equals(fileState.getHash()) && previousFileState.getLastModified() != fileState.getLastModified())
				{
					dateModified.add(new Difference(previousFileState, fileState));
				}
				else
				{
					contentModified.add(new Difference(previousFileState, fileState));
				}
			}
			else if (compareMode != CompareMode.FAST && (samePreviousHash = findSameHash(fileState, previousFileStates)).size() > 0)
			{
				FileState originalFileState = samePreviousHash.get(0);
				if (differences.contains(originalFileState))
				{
					renamed.add(new Difference(originalFileState, fileState));
				}
				else
				{
					duplicated.add(new Difference(originalFileState, fileState));
				}
				differences.remove(originalFileState);
			}
			else
			{
				added.add(new Difference(null, fileState));
			}
		}

		for (FileState fileState : differences)
		{
			deleted.add(new Difference(null, fileState));
		}

		Collections.sort(dateModified);
		Collections.sort(contentModified);
		Collections.sort(renamed);
		Collections.sort(duplicated);
		Collections.sort(added);
		Collections.sort(deleted);

		return this;
	}

	public void displayChanges(boolean verbose)
	{
		if (!verbose)
		{
			displayCounts();
			return;
		}

		String stateFormat = "%-17s ";

		for (Difference diff : dateModified)
		{
			System.out.format(stateFormat + "%s \t%s -> %s%n", "Date modified:", diff.getFileState().getFileName(), formatDate(diff.getPreviousFileState()), formatDate(diff.getFileState()));
		}

		for (Difference diff : contentModified)
		{
			System.out.format(stateFormat + "%s \t%s -> %s%n", "Content modified:", diff.getFileState().getFileName(), formatDate(diff.getPreviousFileState()), formatDate(diff.getFileState()));
		}

		for (Difference diff : renamed)
		{
			System.out.format(String.format(stateFormat + "%s -> %s%n", "Renamed:", diff.getPreviousFileState().getFileName(), diff.getFileState().getFileName()));
		}

		for (Difference diff : duplicated)
		{
			System.out.format(String.format(stateFormat + "%s = %s%n", "Duplicated:", diff.getFileState().getFileName(), diff.getPreviousFileState().getFileName()));
		}

		for (Difference diff : added)
		{
			System.out.format(String.format(stateFormat + "%s%n", "Added:", diff.getFileState().getFileName()));
		}

		for (Difference diff : deleted)
		{
			System.out.format(String.format(stateFormat + "%s%n", "Deleted:", diff.getFileState().getFileName()));
		}

		if (somethingModified())
		{
			System.out.println("");
		}

		displayCounts();
	}

	public void displayCounts()
	{
		if (somethingModified())
		{
			String message = "";
			if (!added.isEmpty())
			{
				message += "" + added.size() + " added, ";
			}

			if (!duplicated.isEmpty())
			{
				message += "" + duplicated.size() + " duplicated, ";
			}

			if (!dateModified.isEmpty())
			{
				message += "" + dateModified.size() + " date modified, ";
			}

			if (!contentModified.isEmpty())
			{
				message += "" + contentModified.size() + " content modified, ";
			}

			if (!renamed.isEmpty())
			{
				message += "" + renamed.size() + " renamed, ";
			}

			if (!deleted.isEmpty())
			{
				message += "" + deleted.size() + " deleted, ";
			}

			message = message.replaceAll(", $", "");
			System.out.println(message);
		}
		else
		{
			System.out.println("Nothing modified");
		}
	}

	public boolean somethingModified()
	{
		return (added.size() + duplicated.size() + dateModified.size() + contentModified.size() + renamed.size() + deleted.size()) > 0;
	}

	private int findSameFileName(FileState search, List<FileState> fileStates)
	{
		int index = 0;
		for (FileState fileState : fileStates)
		{
			if (fileState.getFileName().equals(search.getFileName()))
			{
				return index;
			}
			index++;
		}

		return -1;
	}

	private List<FileState> findSameHash(FileState search, List<FileState> fileStates)
	{
		List<FileState> sameHash = new ArrayList<>();
		for (FileState fileState : fileStates)
		{
			if (fileState.getHash().equals(search.getHash()))
			{
				sameHash.add(fileState);
			}
		}

		return sameHash;
	}

	public List<Difference> getAdded()
	{
		return added;
	}

	public List<Difference> getDuplicated()
	{
		return duplicated;
	}

	public List<Difference> getDateModified()
	{
		return dateModified;
	}

	public List<Difference> getContentModified()
	{
		return contentModified;
	}

	public List<Difference> getRenamed()
	{
		return renamed;
	}

	public List<Difference> getDeleted()
	{
		return deleted;
	}
}
