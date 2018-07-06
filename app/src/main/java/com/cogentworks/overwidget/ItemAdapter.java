package com.cogentworks.overwidget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.woxthebox.draglistview.DragItemAdapter;

import java.util.List;

public class ItemAdapter extends DragItemAdapter<Profile, ItemAdapter.OWViewHolder> {

    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;

    public ItemAdapter(List<Profile> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setItemList(list);
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).Id;
    }

    @Override
    public void onBindViewHolder(@NonNull OWViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Profile profile = mItemList.get(position);
        holder.itemView.setTag(profile);
        Context context = holder.itemView.getContext();
        boolean isDark = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_DARK_THEME, false);

        String[] parts = profile.BattleTag.split("#");
        String username = parts[0];
        holder.name.setText(username);
        if (parts.length > 1) {
            holder.tag.setText("#" + parts[1]);
        }

        if(Integer.parseInt(profile.gamesWon) > 0)
            holder.gamesWon.setText(profile.gamesWon + " games won");

        holder.level.setText("Lvl " + (Integer.parseInt(profile.Prestige)*100 + Integer.parseInt(profile.Level)));
        if (!profile.Tier.equals("") && !profile.Tier.equals("nullrank")) {
            holder.tier.setImageResource(context.getResources().getIdentifier(profile.Tier, "drawable", context.getPackageName()));
            if (isDark)
                holder.comprank.setImageBitmap(WidgetUtils.BuildTextBmp(profile.CompRank, "Dark", context));
            else
                holder.comprank.setImageBitmap(WidgetUtils.BuildTextBmp(profile.CompRank, "Light", context));
        } else {
            holder.itemView.findViewById(R.id.skill_layout).setVisibility(View.GONE);
        }

        String platform = profile.Platform;
        if (!(profile.Region.equals("") || profile.Region.equals("any")))
            platform += " " + profile.Region;

        holder.info.setText(platform);
        try {
            Glide.with(context)
                    .load(profile.AvatarURL)
                    .into((ImageView) holder.itemView.findViewById(R.id.avatar));
        } catch (Exception e) {
            Glide.with(context)
                    .load("https://us.battle.net/forums/static/images/avatars/overwatch/avatar-overwatch-default.png")
                    .into((ImageView) holder.itemView.findViewById(R.id.avatar));
        }
    }

    @NonNull
    @Override
    public OWViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new OWViewHolder(view);
    }

    class OWViewHolder extends DragItemAdapter.ViewHolder {

        TextView name;
        TextView info;
        TextView tag;
        TextView level;
        TextView gamesWon;
        ImageView comprank;
        ImageView tier;

        OWViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);

            name = itemView.findViewById(R.id.name);
            tag = itemView.findViewById(R.id.tag);
            level = itemView.findViewById(R.id.level);
            gamesWon = itemView.findViewById(R.id.games_won);
            info = itemView.findViewById(R.id.info);
            comprank = itemView.findViewById(R.id.appwidget_comprank);
            tier = itemView.findViewById(R.id.appwidget_tier);
        }

        @Override
        public void onItemClicked(View view) {
            final View mView = view;
            AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                    .setTitle("Open in Browser")
                    .setItems(R.array.link_array, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String battleTag = (name.getText().toString() + tag.getText()).replace('#','-');
                            String platform = info.getText().toString().split(" ")[0].toLowerCase();
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            switch (which) {
                                case 0:
                                    i.setData(Uri.parse("http://playoverwatch.com/career/" + platform + "/" + battleTag));
                                    mView.getContext().startActivity(i);
                                    break;
                                case 1:
                                    i.setData(Uri.parse("https://www.overbuff.com/players/" + platform + "/" + battleTag));
                                    mView.getContext().startActivity(i);
                                    break;
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
        }

        @Override
        public boolean onItemLongClicked(View view) {
            Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}