package net.ttddyy.book.libraryservice.checkout;

import java.util.Set;

public record CheckoutBooksDto(
// @formatter:off
		String schoolId,
		long memberId,
		Set<Long> bookIds
		// @formatter:on
) {
}
