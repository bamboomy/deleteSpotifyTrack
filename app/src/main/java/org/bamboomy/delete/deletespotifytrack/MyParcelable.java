/*
 * Copyright (c) 2016 Sander Theetaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.bamboomy.delete.deletespotifytrack;

import android.os.Parcel;
import android.os.Parcelable;

public class MyParcelable implements Parcelable {
	private int mData;

	/* everything below here is for implementing Parcelable */

	// 99.9% of the time you can just ignore this
	public int describeContents() {
		return 0;
	}

	// write your object's data to the passed-in Parcel
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(mData);
	}

	// this is used to regenerate your object. All Parcelables must have a
	// CREATOR that implements these two methods
	public static final Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {
		public MyParcelable createFromParcel(Parcel in) {
			return new MyParcelable(in);
		}

		public MyParcelable[] newArray(int size) {
			return new MyParcelable[size];
		}
	};

	// example constructor that takes a Parcel and gives you an object populated
	// with it's values
	private MyParcelable(Parcel in) {
		mData = in.readInt();
	}
	
	public MyParcelable(int in){
		mData = in;
	}
	
	public int getData() {
		return mData;
	}
}