/*
 * Copyright (c) Jonas Kalderstam 2014.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cowboyprogrammer.org;

import org.cowboyprogrammer.org.parser.OrgParser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class OrgNode {

    // A heading can have any number of sub-headings
    private final List<OrgNode> subNodes;
    // Tags defined on this node
    private final List<String> tags;
    // Timestamps associated with entry
    private final List<OrgTimestamp> timestamps;
    private final List<OrgTimestampRange> timestampRanges;
    private final OrgParser parser;
    // Parent node of this node
    private OrgNode parent = null;
    // Heading level (number of stars). Must be greater than parent.
    // 0 only valid for file object
    private int level = 0;
    // TODO keyword
    private String todo = null;
    // Title of heading (includes anything that was not parsed)
    private String title = "";
    // Body of entry
    private String body = "";
    // Comments before body
    private String comments = "";

    public OrgNode(OrgParser parser) {
        this.parser = parser;
        timestamps = new ArrayList<OrgTimestamp>();
        timestampRanges = new ArrayList<OrgTimestampRange>();
        subNodes = new ArrayList<OrgNode>();
        tags = new ArrayList<String>();
    }

    /**
     * Add all tags.
     */
    public void addTags(final String... tags) {
        if (tags == null) return;

        for (final String tag : tags) {
            this.tags.add(tag);
        }
    }

    /**
     * Add a line to this entry's body. It is parsed and converted to timestamp
     * etc. It is expected to come from BufferedReader's readline and should NOT
     * have an ending newline character!
     */
    public void addBodyLine(final String line) throws ParseException {
        if (line.endsWith("\n")) {
            throw new ParseException("Line should not end with newline!" +
                    " See BufferedReader's readline...", 0);
        }
        // If empty, then we can add timestamps and comments
        if (body.isEmpty() || body.matches("\\A\\s*\\z")) {
            // Check if comment
            if (parser.isCommentLine(line)) {
                setComments(getComments() + line + "\n");
                setBody("");
                return;
            } else if (parser.isTimestampLine(line)) {
                // Don't keep spaces before timestamps
                body = "";
                addTimestamp(parser.getTimestamp(line));
                return;
            } else if (parser.isTimestampRangeLine(line)) {
                // Don't keep spaces before timestamps
                body = "";
                addTimestampRange(parser.getTimestampRange(line));
                return;
            }
        }
        // Nothing happened above, just add to body
        body += line + "\n";
    }

    /**
     * The String representation of this specific entry.
     */
    public String toString() {
        return getOrgHeader() + getOrgBody();
    }

    /**
     * A parser which might have been used to create this object
     */
    public OrgParser getParser() {
        return parser;
    }

    /**
     * Get the header of this entry for org-mode.
     */
    public String getOrgHeader() {
        final StringBuilder sb = new StringBuilder();
        getHeaderString(sb);
        // Remove ending newline
        return sb.toString().trim();
    }

    /**
     * Get body of this entry for org-mode.
     */
    public String getOrgBody() {
        final StringBuilder sb = new StringBuilder();

        sb.append(this.comments);
//        if (this.comments.length() > 0) {
//            sb.append("\n");
//        }

        for (OrgTimestamp t : timestamps) {
            sb.append(t.toString()).append("\n");
        }

        for (OrgTimestampRange t : timestampRanges) {
            sb.append(t.toString()).append("\n");
        }

        sb.append(this.body);

        return sb.toString();
    }

    /**
     * Append the header of this entry.
     * Will end with newline.
     */
    protected void getHeaderString(final StringBuilder sb) {
        // No header without stars
        if (getLevel() < 1) {
            return;
        }

        for (int i = 0; i < getLevel(); i++) {
            sb.append("*");
        }
        sb.append(" ");
        if (this.todo != null) {
            sb.append(this.todo).append(" ");
        }
        sb.append(this.title);
        if (!this.tags.isEmpty()) {
            sb.append(" :");
            for (final String tag : this.tags) {
                sb.append(tag).append(":");
            }
        }
        sb.append("\n");
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int level) {
        if (level < 0) {
            throw new IllegalArgumentException(
                    "Level not allowed to be negative. Only a file can be level 0.");
        }
        this.level = level;
    }

    /**
     * Append the String representation of this specific entry.
     */
    protected void toString(final StringBuilder sb) {
        getHeaderString(sb);
        sb.append(getOrgBody());
    }

    /**
     * Get a String representation of the entire sub tree including
     * this.
     */
    public String treeToString() {
        final StringBuilder sb = new StringBuilder();
        treeToString(sb);
        return sb.toString();
    }

    /**
     * Append a String representation of the entire sub tree including
     * this.
     */
    protected void treeToString(final StringBuilder sb) {
        this.toString(sb);
        for (final OrgNode child : this.subNodes) {
            sb.append("\n");
            child.treeToString(sb);
        }
    }

    /**
     * Tags defined on this node AND any parents.
     */
    public List<String> getAllTags() {
        final List<String> tags = new ArrayList<String>();
        // Add my tags
        tags.addAll(this.tags);
        // And parents.
        OrgNode ancestor = this.parent;
        while (ancestor != null) {
            tags.addAll(ancestor.tags);
            ancestor = ancestor.parent;
        }
        return tags;
    }

    public List<OrgNode> getSubNodes() {
        return subNodes;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<OrgTimestamp> getTimestamps() {
        return timestamps;
    }

    public void addTimestamp(final OrgTimestamp... timestamps) {
        for (final OrgTimestamp ts : timestamps) {
            this.timestamps.add(ts);
        }
    }

    public List<OrgTimestampRange> getTimestampRanges() {
        return timestampRanges;
    }

    public void addTimestampRange(final OrgTimestampRange... timestamps) {
        for (final OrgTimestampRange tr : timestamps) {
            this.timestampRanges.add(tr);
        }
    }

    public String getTodo() {
        return todo;
    }

    public void setTodo(final String todo) {
        this.todo = todo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        if (title == null) {
            throw new NullPointerException("Not allowed to be null!");
        } else if (title.endsWith("\n")) {
            throw new IllegalArgumentException("Title may not end with " +
                    "newline");
        }
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    /**
     * Set the body of this entry. Note that this is not parsed and
     * does not modify existing timestamps etc in this object.
     */
    public void setBody(final String body) {
        if (body == null) {
            throw new NullPointerException("Not allowed to be null!");
        }
        this.body = body;
    }

    public OrgNode getParent() {
        return parent;
    }

    public void setParent(final OrgNode parent) {
        if (parent.getLevel() >= this.level) {
            throw new IllegalArgumentException(
                    "Parent's level must be less than this entry's level!");
        }

        this.parent = parent;
    }

    public String getComments() {
        return comments;
    }

    /**
     * Please note that this function does not modify any other fields.
     */
    public void setComments(final String comments) {
        if (comments == null) {
            throw new NullPointerException("Not allowed to be null!");
        }
        this.comments = comments;
    }
}
