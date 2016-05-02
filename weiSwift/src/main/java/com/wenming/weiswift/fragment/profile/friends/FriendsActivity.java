package com.wenming.weiswift.fragment.profile.friends;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.models.User;
import com.sina.weibo.sdk.openapi.models.UserList;
import com.wenming.weiswift.NewFeature;
import com.wenming.weiswift.R;
import com.wenming.weiswift.common.DetailActivity;
import com.wenming.weiswift.common.endlessrecyclerview.EndlessRecyclerOnScrollListener;
import com.wenming.weiswift.common.endlessrecyclerview.HeaderAndFooterRecyclerViewAdapter;
import com.wenming.weiswift.common.endlessrecyclerview.utils.RecyclerViewStateUtils;
import com.wenming.weiswift.common.endlessrecyclerview.weight.LoadingFooter;
import com.wenming.weiswift.common.util.NetUtil;
import com.wenming.weiswift.common.util.SDCardUtil;
import com.wenming.weiswift.common.util.ToastUtil;

import java.util.ArrayList;

/**
 * Created by wenmingvs on 16/5/1.
 */
public class FriendsActivity extends DetailActivity {

    public RecyclerView mRecyclerView;
    public FriendsAdapter mAdapter;
    public LinearLayoutManager mLayoutManager;
    private HeaderAndFooterRecyclerViewAdapter mHeaderAndFooterRecyclerViewAdapter;
    private ArrayList<User> mDatas;
    private boolean mNoMoreData;
    private String mNext_cursor;

    @Override
    public void initTitleBar() {
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.toolbar_message_detail_base);
        mToolBar = findViewById(R.id.toolbar_home_weiboitem_detail_title);
        mBackIcon = (ImageView) mToolBar.findViewById(R.id.toolbar_back);
        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ((TextView) mToolBar.findViewById(R.id.toolbar_title)).setText("全部关注");
        mToolBar.findViewById(R.id.setting).setVisibility(View.INVISIBLE);
    }

    @Override
    public void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.base_RecyclerView);
        mAdapter = new FriendsAdapter(mDatas, mContext);
        mHeaderAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(mAdapter);
        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mHeaderAndFooterRecyclerViewAdapter);
        //RecyclerViewUtils.setHeaderView(mRecyclerView, new SeachHeadView(mContext));
        //mRecyclerView.addItemDecoration(new WeiboItemSapce((int) mContext.getResources().getDimension(R.dimen.home_weiboitem_space)));
    }

    @Override
    public void pullToRefreshData() {
        mSwipeRefreshLayout.setRefreshing(true);
        mNext_cursor = "0";
        mFriendshipsAPI.friends(Long.parseLong(mAccessToken.getUid()), 50, Integer.valueOf(mNext_cursor), false, new RequestListener() {
            @Override
            public void onComplete(String response) {
                //短时间内疯狂请求数据，服务器会返回数据，但是是空数据。为了防止这种情况出现，要在这里要判空
                if (!TextUtils.isEmpty(response)) {
                    if (NewFeature.CACHE_WEIBOLIST) {
                        SDCardUtil.put(mContext, SDCardUtil.getSDCardPath() + "/weiSwift/", "我的关注列表缓存.txt", response);
                    }
                    mNext_cursor = UserList.parse(response).next_cursor;
                    mDatas = UserList.parse(response).usersList;
                    updateList();
                } else {
                    ToastUtil.showShort(mContext, "网络请求太快，服务器返回空数据，请注意请求频率");
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onWeiboException(WeiboException e) {
                if (NewFeature.CACHE_MESSAGE_COMMENT) {
                    String response = SDCardUtil.get(mContext, SDCardUtil.getSDCardPath() + "/weiSwift/", "我的关注列表缓存.txt");
                    mDatas = UserList.parse(response).usersList;
                    updateList();
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void requestMoreData() {
        mFriendshipsAPI.friends(Long.parseLong(mAccessToken.getUid()), 50, Integer.valueOf(mNext_cursor), false, new RequestListener() {
            @Override
            public void onComplete(String response) {
                if (!TextUtils.isEmpty(response)) {
                    loadMoreData(response);
                } else {
                    ToastUtil.showShort(mContext, "返回的数据为空");
                    mNoMoreData = true;
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onWeiboException(WeiboException e) {
                ToastUtil.showShort(mContext, e.getMessage());
                if (!NetUtil.isConnected(mContext)) {
                    RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.NetWorkError);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void loadMoreData(String string) {
        ArrayList<User> httpRespnse = UserList.parse(string).usersList;
        mNext_cursor = UserList.parse(string).next_cursor;
        mDatas.addAll(httpRespnse);
        updateList();
        RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.Normal);
    }

    @Override
    public void updateList() {
        mRecyclerView.addOnScrollListener(mOnScrollListener);
        mAdapter.setData(mDatas);
        mHeaderAndFooterRecyclerViewAdapter.notifyDataSetChanged();
    }

    public EndlessRecyclerOnScrollListener mOnScrollListener = new EndlessRecyclerOnScrollListener() {
        @Override
        public void onLoadNextPage(View view) {
            super.onLoadNextPage(view);
            LoadingFooter.State state = RecyclerViewStateUtils.getFooterViewState(mRecyclerView);
            if (state == LoadingFooter.State.Loading) {
                Log.d("wenming", "the state is Loading, just wait..");
                return;
            }
            if (!mNoMoreData && mDatas != null) {
                // loading more
                RecyclerViewStateUtils.setFooterViewState(FriendsActivity.this, mRecyclerView, mDatas.size(), LoadingFooter.State.Loading, null);
                requestMoreData();
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    ImageLoader.getInstance().pause();
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                    ImageLoader.getInstance().resume();
                    break;
            }

        }
    };
}
