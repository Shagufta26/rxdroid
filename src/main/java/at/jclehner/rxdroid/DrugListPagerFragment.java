package at.jclehner.rxdroid;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.ScaleXSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.Calendar;
import java.util.Date;

import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;

public class DrugListPagerFragment extends SherlockFragment implements DatePickerDialog.OnDateSetListener,
		NotificationReceiver.OnDoseTimeChangeListener
{
	private class MyPagerAdapter extends FragmentStatePagerAdapter
	{
		public MyPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			final Date date = getDateForPage(position);
			return DrugListFragment.newInstance(date, mPatientId, mDtInfo);
		}

		@Override
		public int getCount() {
			return ADAPTER_ITEMS;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
	};

	private static final String TAG = DrugListPagerFragment.class.getSimpleName();

	public static String ARG_PATIENT_ID = DrugListFragment.ARG_PATIENT_ID;
	public static String ARG_DATE = DrugListFragment.ARG_DATE;

	public static int ADAPTER_ITEMS = 101;
	public static int CENTER_ITEM = 1 + (ADAPTER_ITEMS / 2);

	private ViewPager mPager;

	private int mPatientId;
	private Date mDisplayedDate;
	private Date mDateOrigin;
	private Settings.DoseTimeInfo mDtInfo;

	public void setDate(Date date, boolean force)
	{
		if(date == null)
			throw new NullPointerException();

		if(mDateOrigin.equals(date) && !force)
			return;

		Log.d(TAG, "setDate: date=" + date);

		mDateOrigin = mDisplayedDate = date;
		mDtInfo = Settings.getDoseTimeInfo();

		mPager.getAdapter().notifyDataSetChanged();
		mPager.setCurrentItem(CENTER_ITEM, false);
	}

	@Override
	public void setArguments(Bundle args)
	{
		super.setArguments(args);

		mPatientId = args.getInt(ARG_PATIENT_ID);
		mDateOrigin = mDisplayedDate = (Date) args.getSerializable(ARG_DATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
		setMenuVisibility(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		final int menuResId;

		if(Settings.getBoolean(Settings.Keys.COMPACT_ACTION_BAR, Settings.Defaults.COMPACT_ACTION_BAR))
			menuResId = R.menu.activity_drug_list_compact;
		else
		{
			if(!BuildConfig.DEBUG)
				menuResId = R.id.menu_default_drug_list_activity;
			else
				menuResId = R.menu.activity_drug_list_extended;
		}

		inflater.inflate(menuResId, menu);

		if(Settings.getBoolean(Settings.Keys.USE_SAFE_MODE, false))
			menu.removeItem(R.id.menuitem_take_all);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == R.id.menuitem_date)
		{
			if(mDtInfo.activeDate().equals(mDisplayedDate))
			{
				DatePickerFragment f = DatePickerFragment.newInstance(mDisplayedDate, this);
				f.show(getFragmentManager(), "date");
			}
			else
				setDate(mDtInfo.activeDate(), true);

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDateSet(DatePicker datePicker, int year, int month, int day) {
		setDate(DateTime.date(year, month, day), false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.fragment_drug_list_pager, container, false);
		mPager = ((ViewPager) view.findViewById(R.id.pager));
		mPager.setOffscreenPageLimit(1);
		mPager.setOnPageChangeListener(mPageListener);
		mPager.setAdapter(new MyPagerAdapter(getFragmentManager()));

		setDate(mDateOrigin, true);

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		NotificationReceiver.registerOnDoseTimeChangeListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		NotificationReceiver.unregisterOnDoseTimeChangeListener(this);
	}

	@Override
	public void onDoseTimeBegin(Date date, int doseTime) {
		setDate(date, true);
	}

	@Override
	public void onDoseTimeEnd(Date date, int doseTime) {
		setDate(date, true);
	}

	private Date getDateForPage(int page) {
		return DateTime.add(mDateOrigin, Calendar.DAY_OF_MONTH, page - CENTER_ITEM);
	}

	private void updateDateString()
	{
		final SpannableString dateStr = new SpannableString(DateTime.toNativeDate(mDisplayedDate));
		Util.applyStyle(dateStr, new RelativeSizeSpan(0.75f));

		if(mDtInfo.activeDate().equals(mDisplayedDate))
			Util.applyStyle(dateStr, new UnderlineSpan());

		((SherlockFragmentActivity) getActivity()).getSupportActionBar().setSubtitle(dateStr);
	}

	private final ViewPager.OnPageChangeListener mPageListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int page)
		{
			mDisplayedDate = getDateForPage(page);
			updateDateString();
			Log.d(TAG, "onPageSelected: page=" + page + ", date=" + mDisplayedDate);
		}
	};
}
