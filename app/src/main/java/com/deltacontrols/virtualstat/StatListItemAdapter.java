/* Copyright (c) 2014, Delta Controls Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this 
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this 
list of conditions and the following disclaimer in the documentation and/or other 
materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may 
be used to endorse or promote products derived from this software without specific 
prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
*/
/**
 * StatListItemAdapter.java
 */

package com.deltacontrols.virtualstat;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

/**
 * StatListItemAdapter
 * 
 * A custom ArrayAdapter that allows us to view a list of virtual stats. Applies advanced UI (zebra striping), implements filtering
 */
public class StatListItemAdapter extends ArrayAdapter<StatListItem> implements Filterable {

    private Context context;
    private int layoutResourceId;

    private ArrayList<StatListItem> fullList;
    private ArrayList<StatListItem> filteredList;
    private StatListItemFilter filter;

    @SuppressWarnings("unchecked")
    public StatListItemAdapter(Context context, int layoutResourceId, ArrayList<StatListItem> data) {
        super(context, layoutResourceId, data); // <-- Observable data reference, ArrayAdapter will keep reference to this object
        this.filteredList = data; // We also keep a reference to this for filtering purposes.
        
        // Create copy of the original data; since we are changing our observable data set (filteredList, we need to store the original).
        this.fullList = (ArrayList<StatListItem>) data.clone(); 
        this.layoutResourceId = layoutResourceId;
        this.context = context;
    }

    /**
     * Updates the fullList with a new data set and then notifies itself of the change.
     */
    @SuppressWarnings("unchecked")
    public void updateData(ArrayList<StatListItem> data) {
    	ArrayList<StatListItem> tmpData = (ArrayList<StatListItem>)data.clone();
        this.filteredList.clear(); // Do NOT reset the reference, use existing reference
        this.filteredList.addAll(tmpData);
        this.fullList = tmpData;
        this.notifyDataSetChanged();
    }

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View row, ViewGroup parent) {
        Context ctx = getContext();
        Resources res = ctx.getResources();
        StatListItemHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new StatListItemHolder();
            holder.name = (TextView) row.findViewById(R.id.statlist_statname);

            // Set text colors
            try {
                XmlResourceParser parser = res.getXml(R.drawable.selector_summary_list_text);
                ColorStateList colors = ColorStateList.createFromXml(res, parser);
                holder.name.setTextColor(colors);               
            } catch (Exception e) {
                // Error creating color states, cannot do anything - let default colors be used.
            }

            row.setTag(holder);
        }
        else {
            holder = (StatListItemHolder) row.getTag();
        }

        // Set zebra stripes
        row.setBackgroundDrawable(res.getDrawable((position % 2 == 0)
                ? R.drawable.selector_summary_list_item_1
                : R.drawable.selector_summary_list_item_2));

        StatListItem stat = getItem(position);
        holder.name.setText(stat.name);        
        return row;
    }

    static class StatListItemHolder {
        TextView name;     
    }

    // --------------------------------------------------------------------------------
    // implements Filter
    // --------------------------------------------------------------------------------
    @SuppressLint("DefaultLocale")
    private class StatListItemFilter extends Filter {
        @SuppressLint("DefaultLocale")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            // We implement here the filter logic
            if (constraint == null || constraint.length() == 0) {
                // No constraints' we return the full list
                results.values = fullList;
                results.count = fullList.size();
            }
            else {
                List<StatListItem> resultList = new ArrayList<StatListItem>();

                constraint = constraint.toString().toLowerCase();

                for (StatListItem item : fullList) {
                    if (item.name.toLowerCase().contains(constraint)) {
                        // Filter on name only, not ID
                        resultList.add(item);
                    }
                }

                results.values = resultList;
                results.count = resultList.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Clear and then add all values to force the listAdapter to observe the change; simply setting and then calling notify did not work.
            filteredList.clear();
            filteredList.addAll((ArrayList<StatListItem>) results.values);
            notifyDataSetChanged();
        }
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new StatListItemFilter();

        return filter;
    }
}
