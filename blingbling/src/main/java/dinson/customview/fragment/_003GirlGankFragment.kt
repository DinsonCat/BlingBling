package dinson.customview.fragment

import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import dinson.customview.R
import dinson.customview._global.BaseFragment
import dinson.customview.adapter._003GankGirlsPicAdapter
import dinson.customview.api.GankApi
import dinson.customview.entity.gank.Welfare
import dinson.customview.http.HttpHelper
import dinson.customview.http.RxSchedulers
import dinson.customview.kotlin.logd
import dinson.customview.kotlin.loge
import dinson.customview.manager.GlideSimpleLoader
import dinson.customview.weight._003weight.DecorationLayout
import dinson.customview.weight.imagewatcher.ImageWatcherHelper
import dinson.customview.weight.recycleview.OnRvItemClickListener
import dinson.customview.weight.recycleview.RvItemClickSupport
import dinson.customview.weight.refreshview.CustomRefreshView
import kotlinx.android.synthetic.main.fragment_003_girl_pic_set.*

/**
 * Gank妹子界面
 */
class _003GirlGankFragment : ViewPagerLazyFragment() {

    override fun onCreateView(original: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return original.inflate(R.layout.fragment_003_girl_pic_set, container, false)
    }

    private var mAdapter: _003GankGirlsPicAdapter? = null
    private val mData = ArrayList<Welfare>()
    private var mCurrentPage = 1
    private val mPageSize = 20
    private val mApi by lazy {
        HttpHelper.create(GankApi::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdapter = _003GankGirlsPicAdapter(mData)
        crfGirlsContent.setAdapter(mAdapter)
        crfGirlsContent.setEmptyView("")
    }

    override fun lazyInit() {
        val manager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        manager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        crfGirlsContent.recyclerView.layoutManager = manager

        crfGirlsContent.setOnLoadListener(object : CustomRefreshView.OnLoadListener {
            override fun onRefresh() {
                getDataFromServer(true)
            }

            override fun onLoadMore() {
                getDataFromServer(false)
            }
        })
        crfGirlsContent.isRefreshing = true

        RvItemClickSupport.addTo(crfGirlsContent.recyclerView)
            .setOnItemClickListener(OnRvItemClickListener { _, view, position ->
                layDecoration.attachImageWatcher(mPicHelper)
                if (view is ImageView) {
                    val list = SparseArray<ImageView>()
                    list.put(0, view)
                    mPicHelper.show(view, list, listOf(Uri.parse(mData[position].url)))
                } else {
                    mPicHelper.show(listOf(Uri.parse(mData[position].url)), 0)
                }
            })
    }



    private fun getDataFromServer(isRefresh: Boolean) {
        if (isRefresh) mCurrentPage = 1
        mApi.gankIOGirlsPic(mPageSize, mCurrentPage)
            .doOnNext { data ->
                data.results.forEach {
                    try {
                        val bitmap = Glide.with(this).asBitmap().load(it.url)
                            .submit().get()
                        if (bitmap != null) {
                            it.width = bitmap.width
                            it.height = bitmap.height
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            .compose(RxSchedulers.io_main())
            .subscribe({
                crfGirlsContent.complete()
                if (isRefresh) mData.clear()

                if (it.results == null || it.results.isEmpty())
                    crfGirlsContent.onNoMore()

                mData.addAll(it.results)
                mAdapter?.notifyDataSetChanged()
                mCurrentPage++
            }, {
                it.message?.loge()
                crfGirlsContent.complete()
            }).addToManaged()

    }


    private val mPicHelper by lazy {
        ImageWatcherHelper.with(this.activity, GlideSimpleLoader())
            .setOtherView(layDecoration)
            .addOnShowListener(layDecoration)
    }

    private val layDecoration by lazy { DecorationLayout(context) }

}