package com.openforum.rest.controller.dto;

import java.util.UUID;

public record ThreadResponse(UUID id, String title, String status) {
}
