/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ttddyy.book.libraryservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Tadaya Tsuyukubo
 */
@ConfigurationProperties("library-service")
public class LibraryServiceProperties {

	private Checkout checkout = new Checkout();

	public Checkout getCheckout() {
		return this.checkout;
	}

	public void setCheckout(Checkout checkout) {
		this.checkout = checkout;
	}

	public static class Checkout {

		private int maxBooks;

		private int maxDays;

		public int getMaxBooks() {
			return this.maxBooks;
		}

		public void setMaxBooks(int maxBooks) {
			this.maxBooks = maxBooks;
		}

		public int getMaxDays() {
			return this.maxDays;
		}

		public void setMaxDays(int maxDays) {
			this.maxDays = maxDays;
		}

	}

}
