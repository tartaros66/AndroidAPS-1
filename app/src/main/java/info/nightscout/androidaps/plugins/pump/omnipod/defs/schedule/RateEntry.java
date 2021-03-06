package info.nightscout.androidaps.plugins.pump.omnipod.defs.schedule;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil;
import info.nightscout.androidaps.plugins.pump.omnipod.comm.message.IRawRepresentable;
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodConst;

public class RateEntry implements IRawRepresentable {

    private final double totalPulses;
    // We use a double for the delay between pulses because the Joda time API lacks precision for our calculations
    private final double delayBetweenPulsesInSeconds;

    public RateEntry(double totalPulses, double delayBetweenPulsesInSeconds) {
        this.totalPulses = totalPulses;
        this.delayBetweenPulsesInSeconds = delayBetweenPulsesInSeconds;
    }

    public static List<RateEntry> createEntries(double rate, Duration duration) {
        List<RateEntry> entries = new ArrayList<>();
        int remainingSegments = (int) Math.round(duration.getStandardSeconds() / 1800.0);
        double pulsesPerSegment = (int) Math.round(rate / OmnipodConst.POD_PULSE_SIZE) / 2.0;
        int maxSegmentsPerEntry = pulsesPerSegment > 0 ? (int) (BasalDeliveryTable.MAX_PULSES_PER_RATE_ENTRY / pulsesPerSegment) : 1;

        double durationInHours = duration.getStandardSeconds() / 3600.0;

        double remainingPulses = rate * durationInHours / OmnipodConst.POD_PULSE_SIZE;
        double delayBetweenPulses = 3600 / rate * OmnipodConst.POD_PULSE_SIZE;

        while (remainingSegments > 0) {
            if (rate == 0.0) {
                entries.add(new RateEntry(0, 30D * 60));
                remainingSegments -= 1;
            } else {
                int numSegments = Math.min(maxSegmentsPerEntry, (int) Math.round(remainingPulses / pulsesPerSegment));
                double totalPulses = pulsesPerSegment * numSegments;
                entries.add(new RateEntry(totalPulses, delayBetweenPulses));
                remainingSegments -= numSegments;
                remainingPulses -= totalPulses;
            }
        }

        return entries;
    }

    public double getTotalPulses() {
        return totalPulses;
    }

    public double getDelayBetweenPulsesInSeconds() {
        return delayBetweenPulsesInSeconds;
    }

    @Override
    public byte[] getRawData() {
        byte[] rawData = new byte[0];
        rawData = ByteUtil.concat(rawData, ByteUtil.getBytesFromInt16((int) Math.round(totalPulses * 10)));
        if (totalPulses == 0) {
            rawData = ByteUtil.concat(rawData, ByteUtil.getBytesFromInt((int) (delayBetweenPulsesInSeconds * 1000 * 1000)));
        } else {
            rawData = ByteUtil.concat(rawData, ByteUtil.getBytesFromInt((int) (delayBetweenPulsesInSeconds * 1000 * 100)));
        }
        return rawData;
    }

    @Override
    public String toString() {
        return "RateEntry{" +
                "totalPulses=" + totalPulses +
                ", delayBetweenPulsesInSeconds=" + delayBetweenPulsesInSeconds +
                '}';
    }
}
