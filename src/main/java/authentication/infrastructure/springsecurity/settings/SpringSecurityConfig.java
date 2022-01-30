package authentication.infrastructure.springsecurity.settings;

import authentication.application.TokenBlockListFinder;
import authentication.infrastructure.springsecurity.TokenAuthenticationFilter;
import authentication.infrastructure.springsecurity.TokenAuthenticationProvider;
import com.google.common.collect.ImmutableList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
    private final RequestMatcher PROTECTED_URLS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/users/me/"),
            new AntPathRequestMatcher("/api/users/*/", "PUT"),
            new AntPathRequestMatcher("/api/bookings/*/mp_preferences/"),
            new AntPathRequestMatcher("/api/bookings/", "GET"),
            new AntPathRequestMatcher("/api/bookings/*/"),
            new AntPathRequestMatcher("/api/collaborators/*/"),
            new AntPathRequestMatcher("/api/collaborators/*/roles/"),
            new AntPathRequestMatcher("/api/memberships/*/", "PUT"),
            new AntPathRequestMatcher("/api/memberships/*/", "DELETE"),
            new AntPathRequestMatcher("/api/memberships/*/acquisitions/", "POST"),
            new AntPathRequestMatcher("/api/membership_acquisitions/", "GET"),
            new AntPathRequestMatcher("/api/membership_acquisitions/*/mp_preferences/", "POST"),
            new AntPathRequestMatcher("/api/news/*/", "PUT"),
            new AntPathRequestMatcher("/api/news/*/", "DELETE"),
            new AntPathRequestMatcher("/api/news/*/messages/", "POST"),
            new AntPathRequestMatcher("/api/office_holders/*/"),
            new AntPathRequestMatcher("/api/office_holders/*/office_branches/", "POST"),
            new AntPathRequestMatcher("/api/office_branches/", "GET"),
            new AntPathRequestMatcher("/api/office_branches/*/", "PUT"),
            new AntPathRequestMatcher("/api/office_branches/*/", "DELETE"),
            new AntPathRequestMatcher("/api/office_branches/*/collaborators/"),
            new AntPathRequestMatcher("/api/office_branches/*/memberships/", "POST"),
            new AntPathRequestMatcher("/api/office_branches/*/news/", "POST"),
            new AntPathRequestMatcher("/api/office_branches/*/offices/", "POST"),
            new AntPathRequestMatcher("/api/office_branches/*/roles/"),
            new AntPathRequestMatcher("/api/office_branches/*/reviews/"),
            new AntPathRequestMatcher("/api/office_branches/*/equipments/"),
            new AntPathRequestMatcher("/api/office_branches/*/services/"),
            new AntPathRequestMatcher("/api/offices/*/", "PUT"),
            new AntPathRequestMatcher("/api/offices/*/", "DELETE"),
            new AntPathRequestMatcher("/api/offices/*/bookings/", "POST"),
            new AntPathRequestMatcher("/api/offices/*/bookings/", "GET"),
            new AntPathRequestMatcher("/api/offices/*/inactivities/", "POST"),
            new AntPathRequestMatcher("/api/offices/*/inactivities/", "PUT"),
            new AntPathRequestMatcher("/api/offices/*/services/", "PUT"),
            new AntPathRequestMatcher("/api/offices/*/equipments/", "PUT"),
            new AntPathRequestMatcher("/api/roles/*/"),
            new AntPathRequestMatcher("/api/office_branch_reports/*/total_amount_per_office/"),
            new AntPathRequestMatcher("/api/office_branch_reports/*/total_bookings_per_office/"),
            new AntPathRequestMatcher("/api/office_branch_reports/*/total_amount_per_month/")
    );
    private final RequestMatcher PUBLIC_URLS = new NegatedRequestMatcher(PROTECTED_URLS);
    @Autowired
    private TokenAuthenticationProvider tokenAuthenticationProvider;
    @Autowired
    private TokenBlockListFinder tokenBlockListFinder;
    @Autowired
    private Environment environment;

    public void configure(WebSecurity web) {
        web.ignoring().requestMatchers(PUBLIC_URLS);
    }

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .defaultAuthenticationEntryPointFor(forbiddenEntrypoint(), PROTECTED_URLS)
                .and()
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(restAuthenticationFilter(), AnonymousAuthenticationFilter.class)
                .authorizeRequests()
                .requestMatchers(PROTECTED_URLS)
                .authenticated()
                .and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable();
    }

    @Override
    public void configure(AuthenticationManagerBuilder authBuilder) {
        authBuilder.authenticationProvider(authenticationProvider());
    }

    @Bean
    public TokenAuthenticationFilter restAuthenticationFilter() throws Exception {
        TokenAuthenticationFilter filter = new TokenAuthenticationFilter(
                PROTECTED_URLS,
                tokenBlockListFinder
        );
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationSuccessHandler(successHandler());
        return filter;
    }

    @Bean
    public TokenAuthenticationProvider authenticationProvider() {
        return tokenAuthenticationProvider;
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setRedirectStrategy(new NoRedirectStrategy());
        return successHandler;
    }

    @Bean
    public AuthenticationEntryPoint forbiddenEntrypoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    public FilterRegistrationBean<TokenAuthenticationFilter> disableAutoRegistration(
            final TokenAuthenticationFilter filter
    ) {
        final FilterRegistrationBean<TokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsCustomFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin(environment.getProperty("CLIENT_HOST"));
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.setAllowedMethods(ImmutableList.of("POST", "PUT", "GET", "DELETE"));
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
