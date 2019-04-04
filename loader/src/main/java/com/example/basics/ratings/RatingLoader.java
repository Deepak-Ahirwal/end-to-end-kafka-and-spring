package com.example.basics.ratings;


import com.example.basics.LoadEvent;
import com.example.basics.LoaderBindings;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
class RatingLoader {

	private final ObjectMapper objectMapper;
	private final RatingService ratingService;
	private final MessageChannel channel;
	private final AtomicBoolean load = new AtomicBoolean(false);

	RatingLoader(ObjectMapper objectMapper,
														RatingService ratingService,
														LoaderBindings binding) {
		this.objectMapper = objectMapper;
		this.ratingService = ratingService;
		this.channel = binding.ratings();
	}

	@EventListener(LoadEvent.class)
	public void onLoadEvent() {
		this.load.set(true);
	}

	@Scheduled(fixedRate = 100)
	public void load() throws Exception {
		if (!load.get()) {
			return;
		}
		this.doLoad();
	}

	private void doLoad() throws Exception {

		var rating = this.ratingService.generateRandomRating(2);
		var json = toJson(rating);
		var msg = MessageBuilder.withPayload(json)
			.setHeader(MessageHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.setHeader(KafkaHeaders.MESSAGE_KEY, Integer.toString(rating.getId()).getBytes())
			.build();
		channel.send(msg);
		log.info("sending " + Rating.class.getName() + " " + json);
	}

	@SneakyThrows
	private String toJson(Rating rating) {
		return objectMapper.writeValueAsString(rating);
	}
}


