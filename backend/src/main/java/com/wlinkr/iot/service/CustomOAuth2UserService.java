package com.wlinkr.iot.service;

import com.wlinkr.iot.model.entity.User;
import com.wlinkr.iot.model.enums.AuthProvider;
import com.wlinkr.iot.repository.UserRepository;
import com.wlinkr.iot.security.UserPrincipal;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles OAuth2 user registration / login for Google and Facebook.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(req);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = req.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        String providerId = extractProviderId(attributes, provider);
        String email = extractEmail(attributes, provider);
        String name = extractName(attributes, provider);
        String avatar = extractAvatar(attributes, provider);

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .map(existing -> {
                    existing.setName(name);
                    existing.setAvatarUrl(avatar);
                    existing.setEmail(email);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .avatarUrl(avatar)
                            .provider(provider)
                            .providerId(providerId)
                            .build();
                    return userRepository.save(newUser);
                });

        return UserPrincipal.from(user, attributes);
    }

    // --- Provider-specific attribute extraction ---

    private String extractProviderId(Map<String, Object> attrs, AuthProvider provider) {
        return switch (provider) {
            case GOOGLE   -> (String) attrs.get("sub");
            case FACEBOOK -> (String) attrs.get("id");
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    private String extractEmail(Map<String, Object> attrs, AuthProvider provider) {
        return (String) attrs.get("email");
    }

    private String extractName(Map<String, Object> attrs, AuthProvider provider) {
        return (String) attrs.get("name");
    }

    @SuppressWarnings("unchecked")
    private String extractAvatar(Map<String, Object> attrs, AuthProvider provider) {
        return switch (provider) {
            case GOOGLE   -> (String) attrs.get("picture");
            case FACEBOOK -> {
                Map<String, Object> picture = (Map<String, Object>) attrs.get("picture");
                if (picture != null) {
                    Map<String, Object> data = (Map<String, Object>) picture.get("data");
                    if (data != null) yield (String) data.get("url");
                }
                yield null;
            }
            default -> null;
        };
    }
}
