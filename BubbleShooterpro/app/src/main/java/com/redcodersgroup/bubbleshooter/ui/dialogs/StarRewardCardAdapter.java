package com.redcodersgroup.bubbleshooter.ui.dialogs;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.bubbleshooter.databinding.ItemStarRewardCardBinding;

import java.util.List;

public class StarRewardCardAdapter extends RecyclerView.Adapter<StarRewardCardAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemStarRewardCardBinding binding;

        public ViewHolder(@NonNull ItemStarRewardCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class MilestoneRewardItem {
        public final int milestoneIndex;
        public final int diamondsReward;

        public MilestoneRewardItem(int milestoneIndex, int diamondsReward) {
            this.milestoneIndex = milestoneIndex;
            this.diamondsReward = diamondsReward;
        }
    }

    public interface OnRewardCardActionListener {
        void onCollectCard(int position, MilestoneRewardItem item);
    }

    private final List<MilestoneRewardItem> items;
    private final OnRewardCardActionListener listener;

    public StarRewardCardAdapter(List<MilestoneRewardItem> items, OnRewardCardActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStarRewardCardBinding binding = ItemStarRewardCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MilestoneRewardItem item = items.get(position);
        holder.binding.tvRewardCardTitle.setText("Star Chest #" + item.milestoneIndex);
        holder.binding.tvRewardCardAmount.setText("+" + item.diamondsReward + " DIAMONDS");

        holder.binding.btnCollectCard.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onCollectCard(currentPos, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeCardAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }
    }

    public List<MilestoneRewardItem> getItems() {
        return items;
    }
}
