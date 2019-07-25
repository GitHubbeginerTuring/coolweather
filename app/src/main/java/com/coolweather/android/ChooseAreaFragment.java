package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 0;
    public static final int LEVEL_COUNTY = 0;
    public static final String QUERY_TYPE_PROVINCE = "province";
    public static final String QUERY_TYPE_CITY = "city";
    public static final String QUERY_TYPE_COUNTY = "county";
    public static final String TAG = "ChooseAreaFrag";
    public static final String ADDRESS_OF_QUERY_SERVER="https://guolin.tech/api/china";
    private ProgressDialog mProgressDialog;
    private TextView mTitleText;
    private Button mBackButton;
    private ListView mListView;

    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mDataList = new ArrayList<>();
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;
    private Province mSelectedProvince;
    private City mSelectedCity;
    private int mCurrentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.choose_area, container, false);
        mTitleText = rootView.findViewById(R.id.title_text);
        mBackButton = rootView.findViewById(R.id.back_button);
        mListView = rootView.findViewById(R.id.list_view);
        mArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mDataList);
        mListView.setAdapter(mArrayAdapter);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated: ");
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCurrentLevel == LEVEL_PROVINCE) {
                    mSelectedProvince = mProvinceList.get(position);
                    queryCities();
                } else if (mCurrentLevel == LEVEL_CITY) {
                    mSelectedCity = mCityList.get(position);
                    queryCounties();
                }else if (mCurrentLevel==LEVEL_COUNTY){
                    String weatherId=mCountyList.get(position).getWeatherId();
                    Intent intent=new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        //设置返回按钮事件
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (mCurrentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });

        queryProvinces();//初次加载时显示省级信息
    }

    private void queryProvinces() {
        Log.d(TAG, "queryProvinces: ");
        mTitleText.setText("中国");
        mBackButton.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if (mProvinceList.size() > 0) {
            for (Province p : mProvinceList) {
                mDataList.add(p.getProvinceName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mCurrentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(ADDRESS_OF_QUERY_SERVER, QUERY_TYPE_PROVINCE);
        }
    }

    private void queryCities() {
        mTitleText.setText(mSelectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        mCityList = DataSupport.findAll(City.class);
        if (mCityList.size() > 0) {
            for (City c : mCityList) {
                mDataList.add(c.getCityName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mCurrentLevel = LEVEL_CITY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            String addressOfQueryServer = ADDRESS_OF_QUERY_SERVER+"/" + provinceCode;
            queryFromServer(addressOfQueryServer, QUERY_TYPE_CITY);
        }
    }

    private void queryCounties() {
        mTitleText.setText(mSelectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.findAll(County.class);
        if (mCountyList.size() > 0) {
            for (County c : mCountyList) {
                mDataList.add(c.getCountyName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            mCurrentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            int cityCode = mSelectedCity.getCityCode();
            String addressOfQueryServer = ADDRESS_OF_QUERY_SERVER+"/"
                    + provinceCode + "/" + cityCode;
            queryFromServer(addressOfQueryServer, QUERY_TYPE_COUNTY);
        }
    }

    private void queryFromServer(String addressOfQueryServer, final String type) {
        Log.d(TAG, "queryFromServer: ");
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(addressOfQueryServer, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                Log.d(TAG, "onFailure: ");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "load failed !", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if (type.equals(QUERY_TYPE_PROVINCE)) {
                    Log.d(TAG, "onResponse: ");
                    result = Utility.handleProvinceResponse(responseText);
                } else if (type.equals(QUERY_TYPE_CITY)) {
                    result = Utility.handleCityResponse(responseText, mSelectedProvince.getId());
                } else if (type.equals(QUERY_TYPE_COUNTY)) {
                    result = Utility.handleCountyResponse(responseText, mSelectedCity.getId());
                }
                if (result == true) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (type.equals(QUERY_TYPE_PROVINCE)) {
                                queryProvinces();
                            } else if (type.equals(QUERY_TYPE_CITY)) {
                                queryCities();
                            } else if (type.equals(QUERY_TYPE_COUNTY)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });

    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("loading...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }
}
