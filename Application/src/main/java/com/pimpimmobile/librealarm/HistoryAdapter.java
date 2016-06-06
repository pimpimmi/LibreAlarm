package com.pimpimmobile.librealarm;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.settings.ConfidenceSettings;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final double TREND_UP_DOWN_LIMIT = 1;
    private static final double TREND_SLIGHT_UP_DOWN_LIMIT = 0.5;

    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private List<PredictionData> mHistory;

    private LayoutInflater mInflater;

    private Context mContext;

    private OnListItemClickedListener mListener;

    public HistoryAdapter(Context context, OnListItemClickedListener listener) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mListener = listener;
    }

    public void setHistory(List<PredictionData> history) {
        mHistory = history;
        if (mHistory != null) {
            Collections.sort(mHistory);
            Collections.reverse(mHistory);
        }
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HistoryViewHolder(mInflater.inflate(R.layout.history_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((HistoryViewHolder)holder).onBind(mHistory.get(position));
    }

    @Override
    public int getItemCount() {
        return mHistory == null ? 0 : mHistory.size();
    }

    protected class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView mTimeView;
        TextView mDateView;
        TextView mGlucoseView;
        ImageView mTrendArrow;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onAdapterItemClicked(mHistory.get(getAdapterPosition()).phoneDatabaseId);
                    }
                }
            });
            mTimeView = (TextView) itemView.findViewById(R.id.time);
            mDateView = (TextView) itemView.findViewById(R.id.date);
            mGlucoseView = (TextView) itemView.findViewById(R.id.glucose);
            mTrendArrow = (ImageView) itemView.findViewById(R.id.trend_arrow);
        }

        private void onBind(PredictionData data) {
            boolean alarm = AlgorithmUtil.danger(mContext, data, SettingsUtils.getAlertRules(mContext));
            boolean error = data.errorCode != PredictionData.Result.OK;
            mGlucoseView.setTextColor(error ? Color.YELLOW : (alarm ? Color.RED : Color.WHITE));
            if (error) {
                mGlucoseView.setText("ERR");
                mTrendArrow.setImageDrawable(null);
            } else {
                mGlucoseView.setText(String.valueOf(data.mmolGlucose()));
                updateTrendArrow(data.prediction, data.confidence);
            }

            mTimeView.setText(mTimeFormat.format(new Date(data.realDate)));
            mDateView.setText(mDateFormat.format(new Date(data.realDate)));
        }

        private void updateTrendArrow(double trend, double confidence) {
            float confidenceLimit = Float.valueOf(SettingsUtils.getSettings(
                    mContext, ConfidenceSettings.class.getSimpleName()).getSettingsValue());
            if (confidence > confidenceLimit) {
                mTrendArrow.setImageDrawable(null);
            } else {
                if (trend > TREND_UP_DOWN_LIMIT) {
                    mTrendArrow.setImageResource(R.drawable.ic_arrow_upward_white_24dp);
                } else if (trend < -TREND_UP_DOWN_LIMIT) {
                    mTrendArrow.setImageResource(R.drawable.ic_arrow_downward_white_24dp);
                } else if (trend > TREND_SLIGHT_UP_DOWN_LIMIT) {
                    mTrendArrow.setImageResource(R.drawable.ic_arrow_slight_up_white_24dp);
                } else if (trend < -TREND_SLIGHT_UP_DOWN_LIMIT) {
                    mTrendArrow.setImageResource(R.drawable.ic_arrow_slight_down_white_24dp);
                } else {
                    mTrendArrow.setImageResource(R.drawable.ic_arrow_forward_white_24dp);
                }
            }
        }
    }

    interface OnListItemClickedListener {
        void onAdapterItemClicked(long id);
    }
}
