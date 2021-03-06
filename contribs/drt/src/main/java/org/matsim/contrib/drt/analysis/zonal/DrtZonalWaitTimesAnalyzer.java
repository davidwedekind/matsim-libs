/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.analysis.zonal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer.PerformedRequestEventSequence;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

public final class DrtZonalWaitTimesAnalyzer implements IterationEndsListener {

	private final DrtConfigGroup drtCfg;
	private final DrtRequestAnalyzer requestAnalyzer;
	private final DrtZonalSystem zones;

	public DrtZonalWaitTimesAnalyzer(DrtConfigGroup configGroup, DrtRequestAnalyzer requestAnalyzer,
			DrtZonalSystem zones) {
		this.drtCfg = configGroup;
		this.requestAnalyzer = requestAnalyzer;
		this.zones = zones;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getServices()
				.getControlerIO()
				.getIterationFilename(event.getIteration(), "waitStats" + "_" + drtCfg.getMode() + "_zonal.csv");
		write(fileName);
	}

	public void write(String fileName) {
		String delimiter = ";";
		Map<String, DescriptiveStatistics> zoneStats = createZonalStats(delimiter);
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			DecimalFormat format = new DecimalFormat();
			format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
			format.setMinimumIntegerDigits(1);
			format.setMaximumFractionDigits(2);
			format.setGroupingUsed(false);
			bw.append("zone;centerX;centerY;nRequests;sumWaitTime;meanWaitTime;min;max;p95;p90;p80;p75");
			for (Map.Entry<String, DescriptiveStatistics> zoneStatsEntry : zoneStats.entrySet()) {
				DescriptiveStatistics stats = zoneStatsEntry.getValue();
				bw.newLine();
				bw.append(zoneStatsEntry.getKey()
						+ delimiter
						+ format.format(stats.getN())
						+ delimiter
						+ format.format(stats.getSum())
						+ delimiter
						+ stats.getMean()
						+ delimiter
						+ stats.getMin()
						+ delimiter
						+ stats.getMax()
						+ delimiter
						+ stats.getPercentile(95)
						+ delimiter
						+ stats.getPercentile(90)
						+ delimiter
						+ stats.getPercentile(80)
						+ delimiter
						+ stats.getPercentile(75));
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<String, DescriptiveStatistics> createZonalStats(String delimiter) {
		Map<String, DescriptiveStatistics> zoneStats = new HashMap<>();
		for (PerformedRequestEventSequence seq : requestAnalyzer.getPerformedRequestSequences().values()) {
			if (seq.getPickedUp().isPresent()) {
				DrtZone zone = zones.getZoneForLinkId(seq.getSubmitted().getFromLinkId());
				final String zoneStr = zone != null ?
						zone.getId() + delimiter + zone.getCentroid().getX() + delimiter + zone.getCentroid().getY() :
						"outsideOfDrtZonalSystem;-;-";
				double waitTime = seq.getPickedUp().get().getTime() - seq.getSubmitted().getTime();
				zoneStats.computeIfAbsent(zoneStr, z -> new DescriptiveStatistics()).addValue(waitTime);
			}
		}
		return zoneStats;
	}
}
