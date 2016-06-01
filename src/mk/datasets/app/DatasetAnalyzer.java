package mk.datasets.app;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Kapmat on 2016-05-29.
 */
public class DatasetAnalyzer {

	private static List<Dataset> datasets = new ArrayList<>();
	private static List<Primitive> primitives = new ArrayList<>();
	private static List<Event> events = new ArrayList<>();
	private static LocalDateTime oldestDate;
	private static LocalDateTime newestDate;

	private static int counter = 0;

	public DatasetAnalyzer() {}

	public List<Dataset> getDatasets() {
		return datasets;
	}

	public void resetLists() {
		primitives.clear();
		events.clear();
	}

	public String activePattern(Pattern.Name patternName, String inputE, LocalDateTime startDate, LocalDateTime endDate) {
		String[] inputEvents = inputE.replace(" ","").split(",");

		Event event, secondEvent;
		Pattern pattern = new Pattern();
		pattern.setStartDate(startDate);
		pattern.setEndDate(endDate);

		String outputMsg;
		outputMsg = "\nPoszukiwanie wzorca " + patternName.name() +" dla eventów: ";
		for (int i = 0; i<inputEvents.length; i++) {
			outputMsg = outputMsg + inputEvents[i];
			if (i==(inputEvents.length-1)) {
				outputMsg = outputMsg + ".";
			} else {
				outputMsg = outputMsg + ", ";
			}
		}
		for (int i = 0; i<inputEvents.length; i++) {
			if (inputEvents[i].contains("->")) {
				String[] dubleEvent = inputEvents[i].split("->");
				event = getEventByName(dubleEvent[0]);
				secondEvent = getEventByName(dubleEvent[1]);
			} else {
				event = getEventByName(inputEvents[i]);
				secondEvent = null;
			}
			switch (patternName) {
				case ABSENCE:
					outputMsg = outputMsg + "\n\t" + pattern.absence(event);
					break;
				case INVARIANCE:
					outputMsg = outputMsg + "\n\t" + pattern.invariance(event);
					break;
				case EXISTENCE:
					outputMsg = outputMsg + "\n\t" + pattern.existence(event);
					break;
				case RESPONSE:
					outputMsg = outputMsg + "\n\t" + pattern.response(event, secondEvent);
					break;
				case OBLIGATION:
					outputMsg = outputMsg + "\n\t" + pattern.obligation(event, secondEvent);
					break;
				case RESPONSIVELY:
					outputMsg = outputMsg + "\n\t" + pattern.responsively(event);
					break;
				case PERSISTENCE:
					outputMsg = outputMsg + "\n\t" + pattern.persistence(event);
					break;
				case REACTIVITY:
					outputMsg = outputMsg + "\n\t" + pattern.reactivity(event, secondEvent);
					break;
				default:
					return "ERROR - zły wzorzec";
			}
		}
		return outputMsg;
	}

	public String addDataset(File file) {
		int lastBackslash = file.getAbsolutePath().lastIndexOf('\\');
		String datasetName = file.getAbsolutePath().substring(lastBackslash+1);
		if (getDatasetByName(datasetName)==null) {
			counter++;
			Dataset dataset = new Dataset(counter, datasetName, file.getAbsolutePath());
			datasets.add(dataset);
			setOldestAndNewestDate();
		} else {
			return "Zbiór danych został załadowany już wcześniej!";
		}

		//Sort datasets - first contains the oldest data
		Collections.sort(datasets, (dataset1, dataset2) -> dataset1.getOldestDate().compareTo(dataset2.getOldestDate()));

		assignTimeIdToRecords(0);
		return "Zbiór danych został wczytany poprawnie.";
	}

	private void setOldestAndNewestDate() {
		boolean start = true;
		for (Dataset dataset: datasets) {
			if (start) {
				oldestDate = dataset.getOldestDate();
				newestDate = dataset.getNewestDate();
				start = false;
			} else {
				if (dataset.getNewestDate().isAfter(newestDate)) {
					newestDate = dataset.getNewestDate();
				}
				if (dataset.getOldestDate().isBefore(oldestDate)) {
					oldestDate = dataset.getOldestDate();
				}
			}
		}
	}

	public String addPrimitives(String inputPrimitives) {
		Primitive.resetCounter();
		try {
			if (!inputPrimitives.isEmpty()) {
				String[] primitivesTable = inputPrimitives.split("\\n");

				//Covnert primitives
				List<Primitive> primitiveList = new ArrayList<>();
				for (int i = 0; i < primitivesTable.length; i++) {
					Primitive primitive = Primitive.convertStringToPrimitive(primitivesTable[i]);
					primitive.findRecords(getDatasetById(primitive.getDatasetId()));
					primitive.toString();
					primitiveList.add(primitive);
				}

				//Find duplicates
				if (Primitive.duplicatesExist(primitiveList)) {
					return "ERROR - wykryto duplikaty(Prymitywy). Usuń je i spróbuj ponownie.";
				}
				primitives.addAll(primitiveList);
				return "Prymitywy zostały wczytane poprawnie.";
			}
			return "ERROR - zdefiniuj prymitywy!";
		} catch (Exception e) {
			return "ERROR - prymitywy zostały nieprawidłowo zdefiniowane!";
		}
	}

	public String addEvents(String inputEvents) {
		Event.resetCounter();
		try {
			if (!inputEvents.isEmpty()) {
				String[] eventsTable = inputEvents.split("\\n");

				//Covnert events
				List<Event> eventList = new ArrayList<>();
				for (int i = 0; i < eventsTable.length; i++) {
					Event event = Event.convertStringToEvent(eventsTable[i]);
					event.findDates(datasets, primitives);
					eventList.add(event);
				}

				//Find duplicates
				if (Event.duplicatesExist(eventList)) {
					return "ERROR - wykryto duplikaty(Eventy). Usuń je i spróbuj ponownie.";
				}
				events.addAll(eventList);
				return "Eventy zostały wczytane poprawnie.";
			}
			return "ERROR - zdefiniuj eventy!";
		} catch (Exception e) {
			return "ERROR - eventy zostały nieprawidłowo zdefiniowane!";
		}
	}

	private void assignTimeIdToRecords(int daysDisplacement) {
		int timeId = 0;
		//TODO zmienić daty występowania w przypadku przesunięcia !!!!!
		Dataset firstDataset = datasets.get(0);
		for (Dataset secondDataset: datasets) {
			if (!firstDataset.equals(secondDataset)) {
				for (Record firstRecord: firstDataset.getRecords()) {
					for (Record secondRecord: secondDataset.getRecords()) {
						if (firstRecord.getLocalDateTime().equals(secondRecord.getLocalDateTime()) &&
								firstRecord.getTimeId()==0 && secondRecord.getTimeId()==0) {
							timeId++;
							firstRecord.setTimeId(timeId);
							secondRecord.setTimeId(timeId);
						}
					}
				}
			}
		}
	}

	public static LocalDateTime getOldestDate() {
		return oldestDate;
	}

	public static void setOldestDate(LocalDateTime oldestDate) {
		DatasetAnalyzer.oldestDate = oldestDate;
	}

	public static LocalDateTime getNewestDate() {
		return newestDate;
	}

	public static void setNewestDate(LocalDateTime newestDate) {
		DatasetAnalyzer.newestDate = newestDate;
	}

	public static Dataset getDatasetById(int id) {
		for (Dataset dataset : datasets) {
			if (dataset.getId() == id) {
				return dataset;
			}
		}
		return null;
	}

	public Dataset getDatasetByName(String name) {
		for (Dataset dataset : datasets) {
			if (dataset.getName().equals(name)) {
				return dataset;
			}
		}
		return null;
	}

	public Event getEventByName(String name) {
		for (Event event : events) {
			if (event.getName().equals(name)) {
				return event;
			}
		}
		return null;
	}

	public String showPrimitives() {
		String output = "\n";
		for (Primitive primitive: primitives) {
			output = output + primitive.toString();
			output = output + primitive.showDates();
		}
		return output;
	}

	public String showEvents() {
		String output = "\n";
		for (Event event: events) {
			output = output + event.toString();
			output = output + event.showDates();
		}
		return output;
	}
}
