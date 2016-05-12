package com.pimpimmobile.librealarm;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<PredictionData> mHistory;

    private LayoutInflater mInflater;

    private Context mContext;

    private DecimalFormat mFormat = new DecimalFormat("##.##");

    private ShowDialogListener mListener;

    public HistoryAdapter(Context context, ShowDialogListener listener) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mListener = listener;
    }

    public void setHistory(List<PredictionData> history) {
        mHistory = history;
        notifyDataSetChanged();
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HistoryViewHolder(mInflater.inflate(R.layout.history_item, parent, false));
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        holder.onBind(mHistory.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mHistory == null ? 0 : mHistory.size();
    }

    protected class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView mTitleView;
        TextView mGlucoseView;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.showDialog(mHistory.get(getAdapterPosition()).databaseId);
                    }
                }
            });
            mTitleView = (TextView) itemView.findViewById(R.id.title);
            mGlucoseView = (TextView) itemView.findViewById(R.id.glucose);
        }

        private void onBind(PredictionData data, int position) {
            boolean alarm = AlgorithmUtil.danger(mContext, data, SettingsUtils.getAlertRules(mContext));
            boolean error = data.errorCode != PredictionData.Result.OK;
            if (position == 0 && !error) {
                mGlucoseView.setTextSize(28);
            } else {
                mGlucoseView.setTextSize(20);
            }
            mGlucoseView.setTextColor(error ? Color.YELLOW : (alarm ? Color.RED : Color.WHITE));
            if (error) {
                mGlucoseView.setText(data.errorCode.toString());
                mTitleView.setText(AlgorithmUtil.format(new Date(data.realDate)));
            } else {
                mGlucoseView.setText(String.valueOf(data.mmolGlucose()) + " (" +
                        (data.prediction >= 0 ? "+" : "") +
                        mFormat.format(data.prediction/18f) + ")");
                mTitleView.setText(AlgorithmUtil.format(new Date(data.realDate)) +
                        "\nConfidence: " + mFormat.format(data.confidence));
            }
        }
    }

    interface ShowDialogListener {
        void showDialog(long id);
    }
}
