/*
 * MIT License
 *
 * Copyright (c) 2016 - 2017 Luke Myers (FRC Team 980 ThunderBots)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.team980.thunderscout.info;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentListItem;
import com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder;
import com.bignerdranch.expandablerecyclerview.ViewHolder.ParentViewHolder;
import com.team980.thunderscout.R;
import com.team980.thunderscout.data.ScoutData;
import com.team980.thunderscout.info.statistics.MatchInfoActivity;
import com.team980.thunderscout.info.statistics.TeamInfoActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.team980.thunderscout.info.TeamWrapper.TeamComparator.SORT_TEAM_NUMBER;
import static com.team980.thunderscout.info.TeamWrapper.TeamComparator.getComparator;

public class LocalDataAdapter extends ExpandableRecyclerAdapter<LocalDataAdapter.TeamViewHolder, LocalDataAdapter.MatchViewHolder> {

    private LayoutInflater mInflator;

    private ArrayList<TeamWrapper> teams;

    private Context context;

    private TeamWrapper.TeamComparator sortMode = SORT_TEAM_NUMBER;

    private SparseBooleanArray selectedItems;

    private ThisDeviceFragment fragment;

    public LocalDataAdapter(Context context, @NonNull List<? extends ParentListItem> parentItemList, ThisDeviceFragment f) {
        super(parentItemList);

        mInflator = LayoutInflater.from(context); //TODO move to ViewGroup.getContext()

        teams = (ArrayList<TeamWrapper>) getParentItemList();

        this.context = context;

        selectedItems = new SparseBooleanArray();

        fragment = f;
    }

    // onCreate ...
    @Override
    public TeamViewHolder onCreateParentViewHolder(ViewGroup parentViewGroup) {
        View teamView = mInflator.inflate(R.layout.team_view, parentViewGroup, false);
        return new TeamViewHolder(teamView);
    }

    @Override
    public MatchViewHolder onCreateChildViewHolder(ViewGroup childViewGroup) {
        View scoutView = mInflator.inflate(R.layout.match_view, childViewGroup, false);
        return new MatchViewHolder(scoutView);
    }

    // onBind ...
    @Override
    public void onBindParentViewHolder(TeamViewHolder teamViewHolder, int position, ParentListItem parentListItem) {
        TeamWrapper team = (TeamWrapper) parentListItem;
        teamViewHolder.bind(team);
    }

    @Override
    public void onBindChildViewHolder(MatchViewHolder matchViewHolder, int position, Object childListItem) {
        ScoutData scoutData = (ScoutData) childListItem;
        matchViewHolder.bind(scoutData);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    /**
     * Adds an entry to the view. Called by the database reader.
     *
     * @param data ScoutData to insert
     */
    public void addScoutData(ScoutData data) {

        for (int i = 0; i < teams.size(); i++) {
            TeamWrapper tw = teams.get(i);

            if (tw.getTeamNumber().equals(data.getTeamNumber())) {
                //Pre-existing team

                ArrayList<ScoutData> childList = (ArrayList<ScoutData>) tw.getChildItemList();

                for (ScoutData child : childList) {
                    if (child.getDateAdded() == (data.getDateAdded())) { //TODO verify this works
                        //This child has already been added to the database
                        return;
                    }
                }

                childList.add(data);
                notifyChildItemInserted(i, childList.size() - 1); //TODO verify this
                notifyParentItemChanged(i); //This forces the parent to update

                sort(sortMode);
                return;
            }
        }
        //New team
        teams.add(new TeamWrapper(data.getTeamNumber(), data));
        notifyParentItemInserted(teams.size() - 1); //TODO verify this

        sort(sortMode);
    }

    /**
     * Removes all the data from the list.
     * Called when we delete things.
     */
    public void clearData() {
        if (teams.size() == 0) {
            //list is empty
            return;
        }

        notifyParentItemRangeRemoved(0, teams.size());
        getParentItemList().removeAll(teams);
    }

    public void sort(TeamWrapper.TeamComparator mode) {
        sortMode = mode;

        Collections.sort(teams, getComparator(sortMode));
        notifyParentItemRangeChanged(0, teams.size());
    }

    public TeamWrapper.TeamComparator getCurrentSortMode() {
        return sortMode;
    }

    public void select(int pos) {
        selectedItems.put(pos, true);

        if (!fragment.isInSelectionMode()) {
            //enter selection mode
            fragment.setSelectionMode(true);
            notifyDataSetChanged();
        } else {

            fragment.updateSelectionModeTitle(getSelectedItemCount());
            notifyItemChanged(pos);
        }
    }

    public void deselect(int pos) {
        selectedItems.delete(pos);

        if (getSelectedItemCount() == 0 && fragment.isInSelectionMode()) {
            //exit selection mode
            fragment.setSelectionMode(false);
            notifyDataSetChanged();
        } else {
            fragment.updateSelectionModeTitle(getSelectedItemCount());

            notifyItemChanged(pos);
        }
    }

    public void clearSelections() {
        selectedItems.clear();

        if (fragment.isInSelectionMode()) {
            //exit selection mode
            fragment.setSelectionMode(false);
        }

        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<ScoutData> getSelectedItems() {
        List<ScoutData> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add((ScoutData) mItemList.get(selectedItems.keyAt(i)));
        }
        return items;
    }

    public class TeamViewHolder extends ParentViewHolder {

        private TextView teamNumber;
        private TextView descriptor;

        private ImageButton expandButton;

        public TeamViewHolder(View itemView) {
            super(itemView);

            teamNumber = (TextView) itemView.findViewById(R.id.team_teamNumber);
            descriptor = (TextView) itemView.findViewById(R.id.team_descriptor);

            expandButton = (ImageButton) itemView.findViewById(R.id.team_expandButton);
        }

        public void bind(final TeamWrapper tw) {
            teamNumber.setText("Team " + String.valueOf(tw.getTeamNumber()));
            descriptor.setText(tw.getDescriptor(sortMode));

            if (sortMode == TeamWrapper.TeamComparator.SORT_TEAM_NUMBER) {
                descriptor.setText(tw.getNumberOfMatches() + " matches");
            } else {
                descriptor.setText(tw.getDescriptor(sortMode));
            }

            expandButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isExpanded()) {
                        collapseView();
                        expandButton.setImageResource(R.drawable.ic_expand_more_white_24dp);
                    } else {
                        expandView();
                        expandButton.setImageResource(R.drawable.ic_expand_less_white_24dp);
                    }
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment.isInSelectionMode()) {
                        //TODO selectable teams
                        if (isExpanded()) {
                            collapseView();
                            expandButton.setImageResource(R.drawable.ic_expand_more_white_24dp);
                        } else {
                            expandView();
                            expandButton.setImageResource(R.drawable.ic_expand_less_white_24dp);
                        }
                    } else {
                        Intent launchInfoActivity = new Intent(context, TeamInfoActivity.class);
                        launchInfoActivity.putExtra("com.team980.thunderscout.INFO_AVERAGE_SCOUT", tw.getAverageScoutData());
                        context.startActivity(launchInfoActivity);
                    }
                }
            });
        }
    }

    public class MatchViewHolder extends ChildViewHolder {

        private TextView matchNumber;
        private TextView allianceColor;

        private ImageView matchIcon;

        private CheckBox checkBox;

        public MatchViewHolder(View itemView) {
            super(itemView);

            matchNumber = (TextView) itemView.findViewById(R.id.match_matchNumber);
            allianceColor = (TextView) itemView.findViewById(R.id.match_allianceColor);

            matchIcon = (ImageView) itemView.findViewById(R.id.match_icon);

            checkBox = (CheckBox) itemView.findViewById(R.id.match_checkBox);
        }

        public void bind(final ScoutData scoutData) {
            matchNumber.setText("Match " + scoutData.getMatchNumber());
            allianceColor.setText(scoutData.getAllianceColor().toString());

            matchIcon.setColorFilter(new PorterDuffColorFilter(itemView.getResources().getColor(scoutData.getAllianceColor().getColorPrimary()), PorterDuff.Mode.MULTIPLY));

            if (fragment.isInSelectionMode()) {
                matchIcon.setVisibility(View.GONE);
                checkBox.setVisibility(View.VISIBLE);

                if (selectedItems.get(MatchViewHolder.super.getAdapterPosition())) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }
            } else {
                matchIcon.setVisibility(View.VISIBLE);
                checkBox.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (fragment.isInSelectionMode()) {
                        if (selectedItems.get(MatchViewHolder.super.getAdapterPosition())) {
                            deselect(MatchViewHolder.super.getAdapterPosition());
                            checkBox.setChecked(false);
                        } else {
                            select(MatchViewHolder.super.getAdapterPosition());
                            checkBox.setChecked(true);
                        }
                    } else {
                        Intent launchInfoActivity = new Intent(context, MatchInfoActivity.class);
                        launchInfoActivity.putExtra("com.team980.thunderscout.INFO_SCOUT", scoutData);
                        context.startActivity(launchInfoActivity);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (selectedItems.get(MatchViewHolder.super.getAdapterPosition())) {
                        deselect(MatchViewHolder.super.getAdapterPosition());
                        checkBox.setChecked(false);
                    } else {
                        select(MatchViewHolder.super.getAdapterPosition());
                        checkBox.setChecked(true);
                    }
                    return true;
                }
            });

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (selectedItems.get(MatchViewHolder.super.getAdapterPosition())) {
                        deselect(MatchViewHolder.super.getAdapterPosition());
                        checkBox.setChecked(false);
                    } else {
                        select(MatchViewHolder.super.getAdapterPosition());
                        checkBox.setChecked(true);
                    }
                }
            });
        }
    }


}
