/*
 * Copyright (C) 2025 ant-cave <ANTmmmmm@outlook.com> / <antmmmmm@126.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.ant_cave.eco_wake_sleep_sub.state;

public class ServerState {
    private static State currentStatus = State.SLEEP;
    private static State previousStatus = null;

    public static State get() {
        return currentStatus;
    }

    public static State getPrevious() {
        return previousStatus;
    }

    public static void set(State status) {
        previousStatus = currentStatus;
        currentStatus = status;
    }

    public static boolean hasChanged() {
        return previousStatus != null && previousStatus != currentStatus;
    }
}
