package com.utshob.mybikes.data;

import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.Maintenance;
import com.utshob.mybikes.util.Fmt;

import java.util.ArrayList;
import java.util.List;

/** Decides which reminders exist and which are due. Used by the dashboard and by the WorkManager worker. */
public final class ReminderLogic {
    private ReminderLogic() {}

    public static class Reminder {
        public final String key;     // unique per bike + due value -> prevents duplicate notifications
        public final String title;
        public final String text;
        public final boolean due;

        Reminder(String key, String title, String text, boolean due) {
            this.key = key;
            this.title = title;
            this.text = text;
            this.due = due;
        }
    }

    public static double oilNextKm(Maintenance m) { return m.getOilLastKm() + m.getOilIntervalKm(); }

    public static List<Reminder> evaluate(Bike bike, Maintenance m) {
        List<Reminder> out = new ArrayList<>();
        if (bike == null || m == null || bike.getId() == null) return out;
        String id = bike.getId();
        double odo = bike.getOdometer();

        if (m.getOilIntervalKm() > 0) {
            double next = oilNextKm(m);
            boolean due = odo >= next;
            String text = due
                    ? "Engine oil change due. Odometer " + Fmt.km(odo) + " reached " + Fmt.km(next)
                    : "Engine oil change at " + Fmt.km(next) + " (" + Fmt.km(next - odo) + " left)";
            out.add(new Reminder("oil_" + id + "_" + (long) next, bike.getName() + " · Engine oil", text, due));
        }
        if (m.getAirNextDate() > 0) {
            boolean due = System.currentTimeMillis() >= m.getAirNextDate();
            String text = due ? "Air filter service is due (since " + Fmt.date(m.getAirNextDate()) + ")"
                              : "Air filter service on " + Fmt.date(m.getAirNextDate());
            out.add(new Reminder("airdate_" + id + "_" + m.getAirNextDate(), bike.getName() + " · Air filter", text, due));
        }
        if (m.getAirNextKm() > 0) {
            boolean due = odo >= m.getAirNextKm();
            String text = due ? "Air filter service is due. Odometer reached " + Fmt.km(m.getAirNextKm())
                              : "Air filter service at " + Fmt.km(m.getAirNextKm()) + " (" + Fmt.km(m.getAirNextKm() - odo) + " left)";
            out.add(new Reminder("airkm_" + id + "_" + (long) m.getAirNextKm(), bike.getName() + " · Air filter", text, due));
        }
        return out;
    }
}
