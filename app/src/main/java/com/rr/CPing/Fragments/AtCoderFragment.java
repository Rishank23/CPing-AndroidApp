package com.rr.CPing.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.rr.CPing.Adapters.ContestDetailsRecyclerViewAdapter;
import com.rr.CPing.Handlers.BottomSheetHandler;
import com.rr.CPing.Handlers.DateTimeHandler;
import com.rr.CPing.Handlers.SetRankColorHandler;
import com.rr.CPing.Model.AtCoderUserDetails;
import com.rr.CPing.Model.ContestDetails;
import com.rr.CPing.Model.HiddenContestsClass;
import com.rr.CPing.R;
import com.rr.CPing.SharedPref.SharedPrefConfig;
import com.rr.CPing.database.JSONResponseDBHandler;

import java.util.ArrayList;
import java.util.Calendar;

public class AtCoderFragment extends Fragment {

    private final ArrayList<ContestDetails> ongoingContestsArrayList = new ArrayList<>();
    private final ArrayList<ContestDetails> todayContestsArrayList = new ArrayList<>();
    private final ArrayList<ContestDetails> futureContestsArrayList = new ArrayList<>();

    private View groupFragmentView;
    private TextView atCoderUserName, currentRating, highestRating, currentLevel, currentRank;
    private TextView ongoing_nothing, today_nothing, future_nothing;
    private RecyclerView OngoingRV, TodayRV, FutureRV;
    private ContestDetailsRecyclerViewAdapter ongoingRVA, todayRVA, futureRVA;
    private CardView atCoderGraphCard;
    private SetRankColorHandler setRankColor;

    public AtCoderFragment() {
        // Required empty public constructor
    }

//    @Override
//    public void onResume() {
//        ongoingRVA.notifyDataSetChanged();
//        todayRVA.notifyDataSetChanged();
//        futureRVA.notifyDataSetChanged();
//        super.onResume();
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JSONResponseDBHandler jsonResponseDBHandler = new JSONResponseDBHandler(getContext());
        ArrayList<ContestDetails> contestDetailsArrayList = jsonResponseDBHandler.getPlatformDetails("AtCoder");
        setRankColor = new SetRankColorHandler(getContext());

        for (ContestDetails cd : contestDetailsArrayList) {
            Calendar calendar = Calendar.getInstance();
            new DateTimeHandler().setCalender(calendar, cd.getContestEndTime());
            if (chekInDeleteContests(cd.getContestName(), calendar.getTimeInMillis()))
                continue;
            if (!cd.getIsToday().equals("No")) {
                todayContestsArrayList.add(cd);
            } else if (cd.getContestStatus().equals("CODING")) {
                ongoingContestsArrayList.add(cd);
            } else {
                futureContestsArrayList.add(cd);
            }
        }
    }

    private boolean chekInDeleteContests(String contestName, long endTime) {
        ArrayList<HiddenContestsClass> hiddenContestsArrayList = SharedPrefConfig.readInHiddenContests(getContext());
        for (HiddenContestsClass hcc : hiddenContestsArrayList) {
            if (hcc.getContestName().equals(contestName) && hcc.getContestEndTime() == endTime)
                return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        groupFragmentView = inflater.inflate(R.layout.fragment_at_coder, container, false);

        findViewsByIds();

        if (ongoingContestsArrayList.isEmpty()) {
            ongoing_nothing.setVisibility(View.VISIBLE);
            OngoingRV.setVisibility(View.GONE);
        } else {
            ongoing_nothing.setVisibility(View.GONE);
            OngoingRV.setVisibility(View.VISIBLE);
        }
        if (todayContestsArrayList.isEmpty()) {
            today_nothing.setVisibility(View.VISIBLE);
            TodayRV.setVisibility(View.GONE);
        } else {
            today_nothing.setVisibility(View.GONE);
            TodayRV.setVisibility(View.VISIBLE);
        }
        if (futureContestsArrayList.isEmpty()) {
            future_nothing.setVisibility(View.VISIBLE);
            FutureRV.setVisibility(View.GONE);
        } else {
            future_nothing.setVisibility(View.GONE);
            FutureRV.setVisibility(View.VISIBLE);
        }

        // 0 -> Ongoing
        initialize(0);
        // 1 -> Today
        initialize(1);
        // 2 -> Future
        initialize(2);

//         Ratings and graphs

        AtCoderUserDetails atCoderUserDetails = SharedPrefConfig.readInAtCoderPref(getContext());
        ArrayList<Integer> recentRatingsArrayList = new ArrayList<>();

        if (atCoderUserDetails == null) {
            atCoderUserName.setText("-");
            currentRating.setText("-");
            highestRating.setText("-");
            currentRank.setText("-");
            currentLevel.setText("-");
        } else {
            atCoderUserName.setText(String.format("@%s", atCoderUserDetails.getUserName()));
            currentRating.setText(String.valueOf(atCoderUserDetails.getCurrentRating()));
            highestRating.setText(String.valueOf(atCoderUserDetails.getHighestRating()));
            currentRank.setText(String.valueOf(atCoderUserDetails.getCurrentRank()));
            currentLevel.setText(atCoderUserDetails.getCurrentLevel());
            currentLevel.setTextColor(setRankColor.getAtCoderColor(atCoderUserDetails.getCurrentLevel()));
            recentRatingsArrayList = atCoderUserDetails.getRecentContestRatings();
        }

        GraphView graphView = groupFragmentView.findViewById(R.id.at_coder_graph_view);

        if (recentRatingsArrayList.isEmpty()) atCoderGraphCard.setVisibility(View.GONE);
        else {
            DataPoint[] values = new DataPoint[recentRatingsArrayList.size()];

            int maxY = 0, minY = Integer.MAX_VALUE;
            for (int i = 0; i < recentRatingsArrayList.size(); ++i) {
                int temp = recentRatingsArrayList.get(i);
                maxY = Math.max(maxY, temp);
                minY = Math.min(minY, temp);
                values[i] = new DataPoint(i, temp);
            }

            LineGraphSeries<DataPoint> atCoderSeries = new LineGraphSeries<>(values);
            atCoderSeries.setColor(Color.rgb(179, 145, 255));
            atCoderSeries.setDrawDataPoints(true);
            atCoderSeries.setDataPointsRadius(7);

            graphView.getGridLabelRenderer().setGridColor(getResources().getColor(R.color.graphGridsColor, null));
            graphView.getGridLabelRenderer().setVerticalLabelsColor(getResources().getColor(R.color.graphGridsColor, null));
            graphView.getGridLabelRenderer().setHorizontalLabelsColor(getResources().getColor(R.color.graphGridsColor, null));
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMaxX(recentRatingsArrayList.size());
            graphView.getViewport().setYAxisBoundsManual(true);
            graphView.getViewport().setMaxY(maxY);
            graphView.getViewport().setMinY(minY);

            graphView.addSeries(atCoderSeries);
        }

        // On Item Click Listener (Reminders, Visiting Website)

        ongoingRVA.setOnItemClickListener((platFormName, position) -> new BottomSheetHandler().showBottomSheetDialog(null, ongoingRVA, getContext(), ongoingContestsArrayList, position, getLayoutInflater()));
        todayRVA.setOnItemClickListener((platFormName, position) -> new BottomSheetHandler().showBottomSheetDialog(null, todayRVA, getContext(), todayContestsArrayList, position, getLayoutInflater()));
        futureRVA.setOnItemClickListener((platFormName, position) -> new BottomSheetHandler().showBottomSheetDialog(null, futureRVA, getContext(), futureContestsArrayList, position, getLayoutInflater()));

        return groupFragmentView;
    }

    private void findViewsByIds() {
        atCoderGraphCard = groupFragmentView.findViewById(R.id.atCoder_graph_card_view);

        atCoderUserName = groupFragmentView.findViewById(R.id.at_coder_user_name);
        currentRating = groupFragmentView.findViewById(R.id.atCoder_current_rating);
        highestRating = groupFragmentView.findViewById(R.id.atCoder_max_rating);
        currentLevel = groupFragmentView.findViewById(R.id.atCoder_current_level);
        currentRank = groupFragmentView.findViewById(R.id.atCoder_current_rank);

        ongoing_nothing = groupFragmentView.findViewById(R.id.atCoder_ongoing_nothing);
        today_nothing = groupFragmentView.findViewById(R.id.atCoder_today_nothing);
        future_nothing = groupFragmentView.findViewById(R.id.atCoder_future_nothing);

        OngoingRV = groupFragmentView.findViewById(R.id.atCoder_ongoing_recycler_view);
        TodayRV = groupFragmentView.findViewById(R.id.atCoder_today_recycler_view);
        FutureRV = groupFragmentView.findViewById(R.id.atCoder_future_recyclerView);
    }

    private void initialize(int i) {
        if (i == 0) {
            OngoingRV.setHasFixedSize(true);
            OngoingRV.setLayoutManager(new LinearLayoutManager(getContext()));
            ongoingRVA = new ContestDetailsRecyclerViewAdapter(ongoingContestsArrayList);
            OngoingRV.setAdapter(ongoingRVA);
            ongoingRVA.notifyDataSetChanged();
        } else if (i == 1) {
            TodayRV.setHasFixedSize(true);
            TodayRV.setLayoutManager(new LinearLayoutManager(getContext()));
            todayRVA = new ContestDetailsRecyclerViewAdapter(todayContestsArrayList);
            TodayRV.setAdapter(todayRVA);
            todayRVA.notifyDataSetChanged();
        } else {
            FutureRV.setHasFixedSize(true);
            FutureRV.setLayoutManager(new LinearLayoutManager(getContext()));
            futureRVA = new ContestDetailsRecyclerViewAdapter(futureContestsArrayList);
            FutureRV.setAdapter(futureRVA);
            futureRVA.notifyDataSetChanged();
        }
    }
}